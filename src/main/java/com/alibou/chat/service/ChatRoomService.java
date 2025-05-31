package com.alibou.chat.service;

import com.alibou.chat.model.ChatRoom;
import com.alibou.chat.model.User;
import com.alibou.chat.repository.ChatRoomRepository;
import com.alibou.chat.repository.UserRepository;


import java.time.LocalDateTime;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ChatRoomService {
    @Autowired
    ChatRoomRepository chatRoomRepository;
    @Autowired
    UserRepository userRepository;
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
        
}
