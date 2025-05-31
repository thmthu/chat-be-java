package com.alibou.chat.repository;

import com.alibou.chat.model.ChatRoom;
import com.alibou.chat.model.Message;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.alibou.chat.model.User;
import java.util.Optional;  // Add this import

// This will be AUTO IMPLEMENTED by Spring into a Bean called userRepository
// CRUD refers Create, Read, Update, Delete

public interface ChatRoomRepository extends CrudRepository<ChatRoom, String> {
    @Query("SELECT c FROM ChatRoom c LEFT JOIN FETCH c.participants WHERE c.id = :chatRoomId")
    Optional<ChatRoom> findByChatRoomId(@Param("chatRoomId") String chatRoomId);

    
}