package com.alibou.chat.controller;

import com.alibou.chat.DTO.MessageDeleteResponse;
import com.alibou.chat.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MessageController {

    @Autowired
    private MessageService messageService;
    
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<?> deleteMessage(
            @PathVariable String messageId,
            @RequestHeader("User-ID") String userId) {
        
        try {
            boolean deleted = messageService.deleteMessage(messageId, userId);
            
            if (deleted) {
                return ResponseEntity.ok(new MessageDeleteResponse(true, "Message deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageDeleteResponse(false, "You don't have permission to delete this message"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageDeleteResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageDeleteResponse(false, "Error deleting message: " + e.getMessage()));
        }
    }
}