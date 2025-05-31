package com.alibou.chat.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "chat_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {
    @Id
    private String id;


    @ManyToMany
    @JoinTable(
            name = "chat_room_participants",
            joinColumns = @JoinColumn(name = "chat_room_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> participants = new ArrayList<>();

    @Column(name = "latest_time")
    private LocalDateTime createAt;

    @Column(name = "latest_message")
    private String latestMessage;
    // Optional: Use @ElementCollection for unreadCount mapping if needed
    @ElementCollection
    @CollectionTable(name = "unread_counts", joinColumns = @JoinColumn(name = "chat_room_id"))
    @MapKeyJoinColumn(name = "user_id")
    @Column(name = "count")
    private Map<String, Integer> unreadCount = new HashMap<>();

    @Column(name = "group_name")
    @Basic(optional = true)
    private String groupName;

    @Column(name = "group_avatar")
    @Basic(optional = true)
    private String groupAvatar;

    @Column(name = "lastest_sender")
    @Basic(optional = true)
    private String lastestSender;

}
