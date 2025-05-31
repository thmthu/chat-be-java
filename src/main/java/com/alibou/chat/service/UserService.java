package com.alibou.chat.service;

import com.alibou.chat.DTO.ChatMessageDto;
import com.alibou.chat.DTO.NewestMessageDto;
import com.alibou.chat.model.ChatRoom;
import com.alibou.chat.model.Message;
import com.alibou.chat.model.User;
import com.alibou.chat.repository.ChatRoomRepository;
import com.alibou.chat.repository.MessageRepository;
import com.alibou.chat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private MessageRepository messageRepository;

    public List<NewestMessageDto> getListMessage(String userId) {
        User currentUser = userRepository.findById(userId).orElse(null);
        if (currentUser == null) {
            return null;
        }
        
        List<ChatRoom> currentChatRoom = currentUser.getChatRooms();
        List<NewestMessageDto> newestMessageDtos = new ArrayList<>();
        
        for (ChatRoom chatRoom : currentChatRoom) {
            NewestMessageDto dto = new NewestMessageDto();
            dto.setChatRoomId(chatRoom.getId());
            dto.setLatestMessage(chatRoom.getLatestMessage());
            dto.setSentAt(chatRoom.getCreateAt());
            dto.setUnreadCount(chatRoom.getUnreadCount().getOrDefault(userId, 0));
            
            // Kiểm tra xem có phải là group chat không
            boolean isGroupChat = chatRoom.getParticipants().size() > 2 || 
                                chatRoom.getId().startsWith("group_");
            
            if (isGroupChat) {
                // Đây là group chat
                dto.setSenderName("Group: " + chatRoom.getGroupName()); // Hoặc sử dụng tên nhóm nếu có
                dto.setSenderAvatar("group_avatar.png"); // Avatar mặc định cho nhóm
            } else {
                // Đây là chat 1-1
                for (User participant : chatRoom.getParticipants()) {
                    if (!participant.equals(currentUser)) {
                        dto.setSenderAvatar(participant.getAvatar());
                        dto.setSenderName(participant.getUsername());
                        break; // Chỉ lấy người đầu tiên khác current user
                    }
                }
            }
            
            newestMessageDtos.add(dto);
        }
        newestMessageDtos.sort((a, b) -> {
            if (a.getSentAt() == null) return 1;
            if (b.getSentAt() == null) return -1;
            return b.getSentAt().compareTo(a.getSentAt());
        });
        
        return newestMessageDtos;
}

   // Trong UserService.java
    public List<ChatMessageDto> getMessages(String chatRoomId) {
        List<ChatMessageDto> messages = messageRepository.findChatMessageDtosById(chatRoomId);
        System.out.println("Messages for chat room " + chatRoomId + ": " + messages.size() + " messages found.");
        return messages;
    }
}
