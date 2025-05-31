package com.alibou.chat.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String username;

    private String avatar;

    @Column(unique = true, nullable = false)
    private String email;

    private String bio;

    private String password;

    @ManyToMany(mappedBy = "participants")
    private List<ChatRoom> chatRooms = new ArrayList<>();

    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL)
    private List<Message> sentMessages = new ArrayList<>();

    // @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL)
    // private List<Message> receivedMessages = new ArrayList<>();
}
