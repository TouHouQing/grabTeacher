package com.touhouqing.grabteacherbackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String userType;
    private String realName; // 新增字段

    public AuthResponseDTO(String accessToken, Long id, String username, String email, String userType) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.userType = userType;
    }
    
    public AuthResponseDTO(String accessToken, Long id, String username, String email, String userType, String realName) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.userType = userType;
        this.realName = realName;
    }
} 