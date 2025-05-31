
package com.alibou.chat.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.alibou.chat.DTO.UserBasicInfoDto;
import com.alibou.chat.model.User;

// This will be AUTO IMPLEMENTED by Spring into a Bean called userRepository
// CRUD refers Create, Read, Update, Delete
import java.util.*;

public interface UserRepository extends CrudRepository<User, String> {
    // Thêm vào UserRepository.java
    @Query("SELECT new com.alibou.chat.DTO.UserBasicInfoDto(u.id, u.username) FROM User u")
    List<UserBasicInfoDto> findAllBasicInfo();
}