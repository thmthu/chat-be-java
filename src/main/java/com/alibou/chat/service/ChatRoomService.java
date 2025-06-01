package com.alibou.chat.service;

import com.alibou.chat.model.ChatRoom;
import com.alibou.chat.model.User;
import com.alibou.chat.repository.ChatRoomRepository;
import com.alibou.chat.repository.MessageRepository;
import com.alibou.chat.repository.UserRepository;
import com.alibou.chat.websocket.ChatWebSocketHandler;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatRoomService {
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;
    
    // Remove field injection for webSocketHandler
    private ChatWebSocketHandler webSocketHandler;
    
    // Add setter injection instead to break circular dependency
    @Autowired(required = false)
    public void setWebSocketHandler(ChatWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }
    
    public void setUnreadCount(int unreadCount, String chatRoomId, String userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElse(null);
        if (chatRoom == null) {
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        chatRoom.getUnreadCount().put(userId, unreadCount);
        chatRoomRepository.save(chatRoom);
    }

    @Transactional 
    public ChatRoom createChatRoomWithParticipants(String chatRoomId, List<String> userIds) {
        // Create new chat room
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setId(chatRoomId);
        chatRoom.setCreateAt(LocalDateTime.now());
        
        // Find users and add them as participants
        List<User> participants = new ArrayList<>();
        for (String userId : userIds) {
            System.out.println("===================Param" + userId);  
            userRepository.findById(userId).ifPresent(user -> {
                participants.add(user); // THÊM DÒNG NÀY
                user.getChatRooms().add(chatRoom); // Update both sides of the relationship
            });
        }      
        chatRoom.setParticipants(participants);
        System.out.println(chatRoomId + "========================");
        // Save and return
        return chatRoomRepository.save(chatRoom);
    }
    
    @Transactional
    public ChatRoom addParticipant(String chatRoomId, String userId) {
        ChatRoom chatRoom = chatRoomRepository.findByChatRoomId(chatRoomId)
            .orElseThrow(() -> new RuntimeException("Chat room not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!chatRoom.getParticipants().contains(user)) {
            chatRoom.getParticipants().add(user);
            user.getChatRooms().add(chatRoom); // Update both sides
            chatRoom = chatRoomRepository.save(chatRoom);
            userRepository.save(user); // Save the user to update the relationship
        }
        
        return chatRoom;
    }
    
    @Transactional
    public ChatRoom createGroupChat(String groupName, List<String> userIds) {
        // Tạo chatRoomId cho group chat
        List<String> sortedUserIds = new ArrayList<>(userIds);
        Collections.sort(sortedUserIds);
        String latestSender = userIds.get(userIds.size() - 1);
        
        // Tạo chatRoomId bằng cách nối các userId đã sắp xếp
        String chatRoomId = "group_" + String.join("_", sortedUserIds);    
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setId(chatRoomId);
        chatRoom.setCreateAt(LocalDateTime.now());
        chatRoom.setGroupName(groupName);
        chatRoom.setLatestMessage(groupName + " create!"); // Thiết lập tên nhóm nếu có
        chatRoom.setLastestSender(latestSender);
    
        // Thêm participants
        List<User> participants = new ArrayList<>();
        for (String userId : userIds) {
            userRepository.findById(userId).ifPresent(user -> {
                participants.add(user);
                user.getChatRooms().add(chatRoom); // Cập nhật cả hai phía của mối quan hệ
            });
        }
        chatRoom.setParticipants(participants);
        
        return chatRoomRepository.save(chatRoom);
    }
    
        @Transactional
    public boolean deleteChat(String chatRoomId, String userId) {
        // Find chat room and verify it exists
        ChatRoom chatRoom = chatRoomRepository.findByChatRoomId(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + chatRoomId));
        
        // Find user and verify they exist
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        // Check if user is a participant
        if (!chatRoom.getParticipants().contains(user)) {
            return false; // User not authorized
        }
        
        boolean isGroupChat = chatRoomId.startsWith("group_") || chatRoom.getParticipants().size() > 2;
        
        if (isGroupChat) {
            // For group chats, just remove the user from the group
            chatRoom.getParticipants().remove(user);
            user.getChatRooms().remove(chatRoom);
            
            // If this was the last user, delete the group entirely
            if (chatRoom.getParticipants().isEmpty()) {
                // Delete all messages first
                messageRepository.deleteByGroup(chatRoom);
                chatRoomRepository.delete(chatRoom);
            } else {
                chatRoomRepository.save(chatRoom);
            }
            
            userRepository.save(user);
        } else {
            // For direct chats, delete all messages first, then remove the chat room
            messageRepository.deleteByGroup(chatRoom);
            
            for (User participant : chatRoom.getParticipants()) {
                participant.getChatRooms().remove(chatRoom);
                userRepository.save(participant);
            }
            
            chatRoomRepository.delete(chatRoom);
        }
        
        // Notify other participants via WebSocket if handler is available
        if (webSocketHandler != null) {
            webSocketHandler.notifyChatDeleted(chatRoomId, userId);
        }
        
        return true;
    }
}