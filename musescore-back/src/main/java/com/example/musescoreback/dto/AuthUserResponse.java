package com.example.musescoreback.dto;

public record AuthUserResponse(
        String id,
        String username,
        String email
) {
}
