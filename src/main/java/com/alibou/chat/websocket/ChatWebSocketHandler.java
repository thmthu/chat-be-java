package com.alibou.chat.websocket;

import com.alibou.chat.model.ChatRoom;
import com.alibou.chat.model.Message;
import com.alibou.chat.repository.ChatRoomRepository;
import com.alibou.chat.repository.MessageRepository;
import com.alibou.chat.repository.UserRepository;
import com.alibou.chat.service.ChatRoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.transaction.annotation.Transactional; // Thêm dòng này

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired private MessageRepository messageRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ChatRoomService chatRoomService; // Add this line
    

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

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("🔌 Client connected: " + session.getId());
    }
    @Transactional
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        MessagePayload msg = objectMapper.readValue(payload, MessagePayload.class);
        String chatRoomId = normalizeChatRoomId(msg.getSenderId(), msg.getReceiverId());
        sessionUserMap.put(session.getId(), msg.getSenderId());
        System.out.println(" Received message: " + msg.getContent() + " from " + msg.getSenderId() + " to " + msg.getReceiverId() + "at" + chatRoomId);
        ChatRoom chatRoom = null;
        // Tìm hoặc tạo ChatRoom
        try {
             chatRoom = chatRoomRepository.findByChatRoomId(chatRoomId).orElse(null);
        } catch (Exception e) {
            System.err.println("Error finding chat room: " + e.getMessage());
            e.printStackTrace();}
         if (chatRoom == null) {
            System.out.println("Chat room not found, creating new one: " + chatRoomId);
            // Create new chat room with both participants
            List<String> participants = Arrays.asList(msg.getSenderId(), msg.getReceiverId());
            chatRoom = chatRoomService.createChatRoomWithParticipants(chatRoomId, participants);
            System.out.println("===========================0k 2  ");

        } else {
            // Make sure both users are participants
            chatRoomService.addParticipant(chatRoomId, msg.getSenderId());
            chatRoomService.addParticipant(chatRoomId, msg.getReceiverId());
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
        chatRoomRepository.save(chatRoom);

        // Thêm người vào phòng nếu chưa có
        chatRooms.computeIfAbsent(chatRoomId, k -> new ArrayList<>());
        List<UserSession> sessions = chatRooms.get(chatRoomId);
        if (sessions.stream().noneMatch(s -> s.session.getId().equals(session.getId()))) {
            sessions.add(new UserSession(msg.getSenderId(), session));
        }

        // Gửi cho tất cả user trong chat room
        for (UserSession userSession : sessions) {
            if (userSession.session.isOpen()) {
                userSession.session.sendMessage(new TextMessage(payload));
            }
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = sessionUserMap.remove(session.getId());

        // Xoá khỏi mọi room
        for (List<UserSession> sessions : chatRooms.values()) {
            sessions.removeIf(s -> s.session.getId().equals(session.getId()));
        }

        System.out.println("❌ Client disconnected: " + session.getId() + " (user: " + userId + ")");
    }

    private String normalizeChatRoomId(String userA, String userB) {
        return userA.compareTo(userB) > 0 ? userA + "_" + userB : userB + "_" + userA;
    }

    @Data
    private static class MessagePayload {
        private String senderId;
        private String receiverId;
        private String content;
        private String timestamp;
    }
}
