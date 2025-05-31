package com.alibou.chat.model;
import jakarta.persistence.*;

@Entity
@Table(name = "attachments")
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String fileName;
    private String fileType;
    private String fileUrl; // Đường dẫn hoặc link lưu trữ

    @ManyToOne
    @JoinColumn(name = "message_id")
    private Message message;
}
