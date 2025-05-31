package com.alibou.chat.DTO;


import lombok.Data;
import java.util.List;

@Data
public class CreateGroupChatRequest {
    private String groupName; // Tên nhóm (tùy chọn)
    private List<String> userIds; // Danh sách ID của các thành viên
} 