package com.example.musescoreback.model;

public record UserAccount(
        String id,
        String username,
        String email,
        String passwordHash
) {
}
