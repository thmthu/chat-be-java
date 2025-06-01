package com.alibou.chat.model;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "attachments")
public class Attachment {
    @Id
    private String id;

    private String fileName;
    private String fileType;
    private String fileUrl; // Đường dẫn hoặc link lưu trữ

    @ManyToOne
    @JoinColumn(name = "message_id")
    private Message message;
}