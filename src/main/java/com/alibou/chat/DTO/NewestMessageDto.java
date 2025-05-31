package com.alibou.chat.DTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NewestMessageDto {
    private String chatRoomId;
    private String latestMessage;
    private String senderName;
    private String senderAvatar;
    private LocalDateTime sentAt;
    private Integer unreadCount;
}
