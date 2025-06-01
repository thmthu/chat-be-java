package com.alibou.chat.controller;

import com.alibou.chat.DTO.FileUploadResponse;
import com.alibou.chat.model.Attachment;
import com.alibou.chat.model.ChatRoom;
import com.alibou.chat.model.Message;
import com.alibou.chat.model.User;
import com.alibou.chat.repository.AttachmentRepository;
import com.alibou.chat.repository.ChatRoomRepository;
import com.alibou.chat.repository.MessageRepository;
import com.alibou.chat.repository.UserRepository;
import com.alibou.chat.service.FileStorageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


import java.time.LocalDateTime;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    
    @Autowired
    private MessageRepository messageRepository;

    @Autowired 
    private AttachmentRepository attachmentRepository;
    @PostMapping("/upload-file")
public ResponseEntity<FileUploadResponse> uploadFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam("senderId") String senderId,
        @RequestParam("chatRoomId") String chatRoomId,
        @RequestHeader("User-ID") String userId,
        @RequestHeader("Chat-Room-ID") String headerChatRoomId) {
    
    try {
        System.out.println("Received file: " + file.getOriginalFilename());
        // Verify that the user exists
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify that the chat room exists
        ChatRoom chatRoom = chatRoomRepository.findByChatRoomId(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
        
        // Verify that the user is a participant in the chat room
        if (!chatRoom.getParticipants().contains(sender)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new FileUploadResponse(null, null, "User is not a participant in this chat room"));
        }
        
        // Check if file size is within limit (10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(new FileUploadResponse(null, null, "File size exceeds the 10MB limit"));
        }
        
        // Generate a unique file ID first
        String fileId = UUID.randomUUID().toString();
        
        // Store the file with this ID
        String originalFileName = file.getOriginalFilename();
        fileStorageService.storeFile(file, fileId);
        
        // Generate the file URL using the fileId
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/files/")
                .path(fileId)
                .toUriString();
        System.out.println("File download URI: " + fileDownloadUri);
        // Create a message associated with this file
        Message message = new Message();
        message.setContent("FILE="+ originalFileName + "=" + fileDownloadUri);
        message.setSender(sender);
        message.setGroup(chatRoom);
        message.setSentAt(LocalDateTime.now());
        
        // Save the message
        Message savedMessage = messageRepository.save(message);
        System.out.println("Message saved with ID: " + savedMessage.getId());
        // Create and save the attachment
        Attachment attachment = new Attachment();
        attachment.setId(fileId);
        attachment.setFileName(originalFileName);
        attachment.setFileType(file.getContentType());
        attachment.setFileUrl(fileDownloadUri);
        attachment.setMessage(savedMessage);
        
        // Save the attachment
        attachmentRepository.save(attachment);
        System.out.println("Attachment saved with ID: " + attachment.getId());
        
        // Update chat room with latest message info
        chatRoom.setLatestMessage("[FILE] " + originalFileName);
        chatRoom.setLastestSender(senderId);
        chatRoom.setCreateAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);
        System.out.println("Chat room updated with latest message: " + chatRoom.getLatestMessage());
        // Create response
        FileUploadResponse response = new FileUploadResponse(
                fileId,
                fileDownloadUri,
                originalFileName
        );
        
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new FileUploadResponse(null, null, "Error: " + e.getMessage()));
    }
}
@GetMapping("/files/{fileId}")
public ResponseEntity<?> downloadFile(
        @PathVariable String fileId,
        @RequestHeader(value = "User-ID", required = false) String userId) {
    
    try {
        // Find the attachment by ID
        Attachment attachment = attachmentRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        
        // Get the file path from the storage service
        Path filePath = fileStorageService.getFilePath(fileId);
        
        if (!Files.exists(filePath)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("File not found on server");
        }
        
        // Determine content type
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream"; // Default content type for unknown files
        }
        
        // Read file content
        byte[] fileContent = Files.readAllBytes(filePath);
        
        // Return the file with appropriate headers
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + attachment.getFileName() + "\"")
                .body(fileContent);
        
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error downloading file: " + e.getMessage());
    }
}
}