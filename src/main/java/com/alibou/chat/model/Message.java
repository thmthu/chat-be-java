package com.alibou.chat.model;
import com.alibou.chat.model.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String content;
    private LocalDateTime sentAt;
    @PrePersist
    public void prePersist() {
        this.sentAt = LocalDateTime.now();
    }
    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;


    // Tin nhắn trong group chat
    @ManyToOne
    @JoinColumn(name = "chat_room_id")
    private ChatRoom group;

    @OneToMany(mappedBy = "message")
    private List<Attachment> attachments;
}
