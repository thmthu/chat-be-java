package com.alibou.chat.controller;

import com.alibou.chat.DTO.CreateGroupChatRequest;
import com.alibou.chat.model.ChatRoom;
import com.alibou.chat.service.ChatRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    
    @DeleteMapping("/delete-chat/{chatRoomId}")
    public ResponseEntity<?> deleteChat(
            @PathVariable String chatRoomId,
            @RequestHeader("User-ID") String userId) {
        
        try {
            // Check if chat exists and user is participant
            boolean isDeleted = chatRoomService.deleteChat(chatRoomId, userId);
            
            if (isDeleted) {
                return ResponseEntity.ok().body("Chat deleted successfully");
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You don't have permission to delete this chat");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Chat room not found: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error: " + e.getMessage());
        }
    }
}