package com.example.musescoreback.dto;

public record AuthResponse(
        String token,
        AuthUserResponse user
) {
}
