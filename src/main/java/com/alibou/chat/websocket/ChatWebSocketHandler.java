package com.alibou.chat.websocket;

import com.alibou.chat.model.ChatRoom;
import com.alibou.chat.model.Message;
import com.alibou.chat.model.User;
import com.alibou.chat.repository.ChatRoomRepository;
import com.alibou.chat.repository.MessageRepository;
import com.alibou.chat.repository.UserRepository;
import com.alibou.chat.service.ChatRoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired private MessageRepository messageRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private UserRepository userRepository;
    @Autowired @Lazy  private ChatRoomService chatRoomService;
    
    private static class UserSession {
        String userId;
        WebSocketSession session;

        public UserSession(String userId, WebSocketSession session) {
            this.userId = userId;
            this.session = session;
        }
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Map<chatRoomId, List<UserSession>>
    private final Map<String, List<UserSession>> chatRooms = new ConcurrentHashMap<>();

    // Map<sessionId, userId>
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    
    // Store all active WebSocket sessions
    private final List<WebSocketSession> allSessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("🔌 Client connected: " + session.getId());
        allSessions.add(session);
        
        // Extract userId from URL query parameters
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            String userId = extractUserIdFromQuery(query);
            if (userId != null) {
                sessionUserMap.put(session.getId(), userId);
                System.out.println("User " + userId + " connected with session " + session.getId());
            }
        }
    }

    // Helper method to extract userId from query string
    private String extractUserIdFromQuery(String query) {
        // Handle query string like: "userId=123" or "userId=123&param2=value2"
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            if (pair.startsWith("userId=")) {
                return pair.substring(7); // "userId=".length() == 7
            }
        }
        return null;
    }
    
    @Transactional
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        MessagePayload msg = objectMapper.readValue(payload, MessagePayload.class);
        
        // Tìm tên người gửi từ senderId và thêm vào payload
        userRepository.findById(msg.getSenderId()).ifPresent(user -> {
            msg.setSenderName(user.getUsername());
        });
        
        // Cập nhật payload với thông tin senderName mới
        payload = objectMapper.writeValueAsString(msg);
        
        // Xác định chatRoomId (ưu tiên từ payload nếu có)
        String chatRoomId;
        boolean isGroupChat = false;
        
        if (msg.getChatRoomId() != null && !msg.getChatRoomId().isEmpty()) {
            chatRoomId = msg.getChatRoomId();
            isGroupChat = chatRoomId.startsWith("group_");
        } else {
            // Tạo chatRoomId cho chat 1-1
            chatRoomId = normalizeChatRoomId(msg.getSenderId(), msg.getReceiverId());
        }
        
        sessionUserMap.put(session.getId(), msg.getSenderId());
        System.out.println("📩 Received message: " + msg.getContent() + " from " + msg.getSenderId() + 
                          " to room " + chatRoomId);
        
        ChatRoom chatRoom = null;
        
        // Tìm hoặc tạo ChatRoom
        try {
            chatRoom = chatRoomRepository.findByChatRoomId(chatRoomId).orElse(null);
        } catch (Exception e) {
            System.err.println("Error finding chat room: " + e.getMessage());
            e.printStackTrace();
        }
        
        if (chatRoom == null) {
            // Đối với group chat, không tạo mới (nên đã tồn tại)
            if (isGroupChat) {
                throw new RuntimeException("Group chat " + chatRoomId + " not found");
            }
            
            System.out.println("Chat room not found, creating new one: " + chatRoomId);
            // Tạo chat room mới cho chat 1-1
            List<String> participants = Arrays.asList(msg.getSenderId(), msg.getReceiverId());
            chatRoom = chatRoomService.createChatRoomWithParticipants(chatRoomId, participants);
        } else {
            // Đảm bảo sender là participant
            chatRoomService.addParticipant(chatRoomId, msg.getSenderId());
            
            // Nếu là chat 1-1, thêm cả receiver
            if (!isGroupChat && msg.getReceiverId() != null) {
                chatRoomService.addParticipant(chatRoomId, msg.getReceiverId());
            }
        }
        
        // Lưu message mới
        System.out.println("===========================0k   ");
        Message newMessage = new Message();
        newMessage.setContent(msg.getContent());
        newMessage.setSentAt(java.time.LocalDateTime.now());
        newMessage.setSender(userRepository.findById(msg.getSenderId()).orElse(null));
        newMessage.setGroup(chatRoom);
        messageRepository.save(newMessage);
    
        // Cập nhật latestMessage trong ChatRoom
        chatRoom.setLatestMessage(msg.getContent());
        chatRoom.setLastestSender(msg.getSenderId());
        chatRoom.setCreateAt(java.time.LocalDateTime.now());
        chatRoomRepository.save(chatRoom);
    
        // Đối với group chat, gửi tin nhắn đến tất cả user trong group
        if (isGroupChat) {
            sendMessageToGroupParticipants(chatRoomId, payload, session);
        } else {
            // Xử lý chat 1-1 
            chatRooms.computeIfAbsent(chatRoomId, k -> new ArrayList<>());
            List<UserSession> sessions = chatRooms.get(chatRoomId);
            
            // Thêm session hiện tại vào phòng nếu chưa có
            if (sessions.stream().noneMatch(s -> s.session.getId().equals(session.getId()))) {
                sessions.add(new UserSession(msg.getSenderId(), session));
            }
            
            // Lấy ID của người nhận
            String receiverId = msg.getReceiverId();
            
            // Tìm tất cả các session đang hoạt động của cả người gửi và người nhận
            List<WebSocketSession> activeSessionsToNotify = new ArrayList<>();
            
            // Thêm session hiện tại
            activeSessionsToNotify.add(session);
            
            // Tìm session của người nhận từ sessionUserMap
            for (Map.Entry<String, String> entry : sessionUserMap.entrySet()) {
                String sessionId = entry.getKey();
                String userId = entry.getValue();
                
                // Nếu là session của người nhận
                if (userId.equals(receiverId)) {
                    // Tìm WebSocketSession tương ứng từ allSessions
                    for (WebSocketSession wsSession : allSessions) {
                        if (wsSession.getId().equals(sessionId) && wsSession.isOpen()) {
                            // Thêm vào chatRoom và danh sách để gửi tin nhắn
                            sessions.add(new UserSession(receiverId, wsSession));
                            activeSessionsToNotify.add(wsSession);
                        }
                    }
                }
            }
            
            // Gửi tin nhắn đến tất cả session
            for (WebSocketSession targetSession : activeSessionsToNotify) {
                if (targetSession.isOpen()) {
                    try {
                        targetSession.sendMessage(new TextMessage(payload));
                    } catch (IOException e) {
                        System.err.println("Error sending message: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    // Thêm phương thức để gửi tin nhắn đến tất cả người tham gia trong group
    private void sendMessageToGroupParticipants(String groupId, String payload, WebSocketSession senderSession) {
        // Lấy tất cả participant trong group chat
        chatRoomRepository.findByChatRoomId(groupId).ifPresent(chatRoom -> {
            List<String> participantIds = chatRoom.getParticipants().stream()
                .map(User::getId)
                .toList();
            
            // Thêm người gửi vào phòng nếu chưa có
            String senderId = sessionUserMap.get(senderSession.getId());
            chatRooms.computeIfAbsent(groupId, k -> new ArrayList<>());
            List<UserSession> roomSessions = chatRooms.get(groupId);
            
            // Add sender's session to the room if not already there
            if (roomSessions.stream().noneMatch(s -> s.session.getId().equals(senderSession.getId()))) {
                roomSessions.add(new UserSession(senderId, senderSession));
            }
            
            // Find active sessions for all participants
            for (String participantId : participantIds) {
                // Find all sessions for this participant
                for (Map.Entry<String, String> entry : sessionUserMap.entrySet()) {
                    String sessionId = entry.getKey();
                    String userId = entry.getValue();
                    
                    if (participantId.equals(userId)) {
                        // Find the actual WebSocketSession
                        for (WebSocketSession wsSession : allSessions) {
                            if (wsSession.getId().equals(sessionId) && wsSession.isOpen()) {
                                // Add to room sessions if not already there
                                if (roomSessions.stream().noneMatch(s -> 
                                        s.session.getId().equals(wsSession.getId()))) {
                                    roomSessions.add(new UserSession(participantId, wsSession));
                                }
                            }
                        }
                    }
                }
            }
            
            // Send message to all sessions in the room
            for (UserSession userSession : roomSessions) {
                if (userSession.session.isOpen()) {
                    try {
                        userSession.session.sendMessage(new TextMessage(payload));
                    } catch (IOException e) {
                        System.err.println("Error sending message: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = sessionUserMap.remove(session.getId());
        allSessions.remove(session);

        // Remove from all rooms
        for (List<UserSession> sessions : chatRooms.values()) {
            sessions.removeIf(s -> s.session.getId().equals(session.getId()));
        }

        System.out.println("❌ Client disconnected: " + session.getId() + " (user: " + userId + ")");
    }

    private String normalizeChatRoomId(String userA, String userB) {
        return userA.compareTo(userB) > 0 ? userA + "_" + userB : userB + "_" + userA;
    }
    // ... existing code ...

/**
 * Notifies all participants in a chat room that it has been deleted
 */
public void notifyChatDeleted(String chatRoomId, String deletedByUserId) {
    try {
        // Create a notification payload
        MessagePayload payload = new MessagePayload();
        payload.setSenderId("system");
        payload.setChatRoomId(chatRoomId);
        payload.setContent("CHAT_DELETED");
        payload.setSenderName("System");
        payload.setTimestamp(LocalDateTime.now().toString());
        
        // Convert to JSON
        String message = objectMapper.writeValueAsString(payload);
        
        // Get all sessions in this chat room
        List<UserSession> sessions = chatRooms.getOrDefault(chatRoomId, new ArrayList<>());
        
        // Send to all participants except the one who deleted it
        for (UserSession userSession : sessions) {
            if (!userSession.userId.equals(deletedByUserId) && userSession.session.isOpen()) {
                userSession.session.sendMessage(new TextMessage(message));
            }
        }
        
        // Remove this chat room from the map
        chatRooms.remove(chatRoomId);
        
    } catch (IOException e) {
        System.err.println("Error notifying chat deletion: " + e.getMessage());
    }
}

// ... rest of existing code ...

    @Data
    private static class MessagePayload {
        private String senderId;
        private String receiverId;
        private String content;
        private String timestamp;
        private String chatRoomId;
        private String senderName;
    }
}