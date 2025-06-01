package com.alibou.chat.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {
    private String chatRoomId;
    private String senderId;
    private String content;
    private LocalDateTime sentAt;
    private String senderName;
    private String senderAvatar;
    // private String messageId ;
    // private String messageType; // "text", "image", etc.
    
}
