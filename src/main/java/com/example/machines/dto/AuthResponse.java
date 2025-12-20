package com.example.machines.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private boolean success;
    private String message;
    private AuthData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthData {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private String role;
        private String token;
        private String createdAt;
    }
}

