
package com.alibou.chat.service;

import com.alibou.chat.model.ChatRoom;
import com.alibou.chat.model.Message;
import com.alibou.chat.model.User;
import com.alibou.chat.repository.ChatRoomRepository;
import com.alibou.chat.repository.MessageRepository;
import com.alibou.chat.repository.UserRepository;
import com.alibou.chat.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    
    @Autowired
    private ChatWebSocketHandler webSocketHandler;
    
    /**
     * Deletes a message and notifies all users in the chat room
     * 
     * @param messageId ID of the message to delete
     * @param userId ID of the user requesting deletion
     * @return true if deleted successfully, false if user doesn't have permission
     * @throws IllegalArgumentException if message not found
     */
    @Transactional
    public boolean deleteMessage(String messageId, String userId) {
        // Find the message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        
        // Find the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Check if user has permission to delete (must be the sender)
        if (!message.getSender().getId().equals(userId)) {
            // Alternative: Check if user is an admin or group owner for group chats
            return false;
        }
        
        // Get the chat room ID for notification before deleting
        ChatRoom chatRoom = message.getGroup();
        String chatRoomId = chatRoom.getId();
        
        // Delete the message
        messageRepository.delete(message);
        
        // Notify all users in the chat room
        notifyMessageDeleted(chatRoomId, messageId, userId);
        
        return true;
    }
    
    /**
     * Notifies all users in a chat room that a message was deleted
     */
    private void notifyMessageDeleted(String chatRoomId, String messageId, String deletedByUserId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "MESSAGE_DELETED");
        notification.put("chatRoomId", chatRoomId);
        notification.put("messageId", messageId);
        notification.put("deletedBy", deletedByUserId);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("senderId", deletedByUserId); // Add this line for client compatibility

        webSocketHandler.sendMessageToAllChatParticipants(chatRoomId, notification);
    }
}
