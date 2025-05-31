package com.alibou.chat.controller;

import com.alibou.chat.DTO.CreateGroupChatRequest;
import com.alibou.chat.model.ChatRoom;
import com.alibou.chat.service.ChatRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatController {

    @Autowired
    private ChatRoomService chatRoomService;
    
    @PostMapping("/create-group")
    public ResponseEntity<?> createGroupChat(@RequestBody CreateGroupChatRequest request) {
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            return ResponseEntity.badRequest().body("Danh sách thành viên không được để trống");
        }
        
        ChatRoom groupChat = chatRoomService.createGroupChat(
            request.getGroupName(),
            request.getUserIds()
        );
        
        return ResponseEntity.ok(groupChat);
    }
}