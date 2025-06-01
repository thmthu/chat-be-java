package com.alibou.chat.repository;

import com.alibou.chat.DTO.ChatMessageDto;
import com.alibou.chat.model.ChatRoom;
import com.alibou.chat.model.Message;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.alibou.chat.model.User;

import java.util.List;

// This will be AUTO IMPLEMENTED by Spring into a Bean called userRepository
// CRUD refers Create, Read, Update, Delete

public interface MessageRepository extends CrudRepository<Message, String> {    // Trong MessageRepository.java
@Query("SELECT new com.alibou.chat.DTO.ChatMessageDto(g.id, u.id, m.content, m.sentAt, u.username, u.avatar, m.id) " +
       "FROM Message m JOIN m.group g JOIN m.sender u " +
       "WHERE g.id = :id ORDER BY m.sentAt ASC")
List<ChatMessageDto> findChatMessageDtosById(@Param("id") String chatRoomId);
void deleteByGroup(ChatRoom chatRoom);

}
