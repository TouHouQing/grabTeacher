package com.touhouqing.grabteacherbackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String userType;

    public AuthResponse(String accessToken, Long id, String username, String email, String userType) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.userType = userType;
    }
} 