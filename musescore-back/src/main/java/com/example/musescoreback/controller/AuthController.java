package com.example.musescoreback.controller;

import com.example.musescoreback.dto.AuthResponse;
import com.example.musescoreback.dto.AuthUserResponse;
import com.example.musescoreback.dto.LoginRequest;
import com.example.musescoreback.dto.RegisterRequest;
import com.example.musescoreback.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/me")
    public AuthUserResponse me(@RequestHeader(name = "Authorization", required = false) String authorization) {
        return authService.me(authorization);
    }
}
