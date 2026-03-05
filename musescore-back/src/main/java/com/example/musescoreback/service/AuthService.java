package com.example.musescoreback.service;

import com.example.musescoreback.dto.AuthResponse;
import com.example.musescoreback.dto.AuthUserResponse;
import com.example.musescoreback.dto.LoginRequest;
import com.example.musescoreback.dto.RegisterRequest;
import com.example.musescoreback.exception.BizException;
import com.example.musescoreback.model.UserAccount;
import com.example.musescoreback.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserAccountRepository userAccountRepository, JwtService jwtService) {
        this.userAccountRepository = userAccountRepository;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        UserAccount user = userAccountRepository.findByAccount(request.account())
                .orElseThrow(() -> new BizException(HttpStatus.UNAUTHORIZED, "用户名/邮箱或密码错误"));

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new BizException(HttpStatus.UNAUTHORIZED, "用户名/邮箱或密码错误");
        }

        String token = jwtService.generateToken(user.id());
        return new AuthResponse(token, toAuthUser(user));
    }

    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (!StringUtils.hasText(username) || !StringUtils.hasText(email) || !StringUtils.hasText(request.password())) {
            throw new BizException(HttpStatus.BAD_REQUEST, "注册参数不完整");
        }

        if (userAccountRepository.existsByUsername(username)) {
            throw new BizException(HttpStatus.CONFLICT, "用户名已存在");
        }

        if (userAccountRepository.existsByEmail(email)) {
            throw new BizException(HttpStatus.CONFLICT, "邮箱已被注册");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        UserAccount user = userAccountRepository.insert(username, email, passwordHash);
        String token = jwtService.generateToken(user.id());
        return new AuthResponse(token, toAuthUser(user));
    }

    public AuthUserResponse me(String bearerToken) {
        String token = extractBearerToken(bearerToken);
        String userId = jwtService.parseUserId(token)
                .orElseThrow(() -> new BizException(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录"));

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BizException(HttpStatus.UNAUTHORIZED, "用户不存在"));

        return toAuthUser(user);
    }

    private String extractBearerToken(String bearerToken) {
        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) {
            throw new BizException(HttpStatus.UNAUTHORIZED, "缺少认证令牌");
        }
        String token = bearerToken.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            throw new BizException(HttpStatus.UNAUTHORIZED, "缺少认证令牌");
        }
        return token;
    }

    private AuthUserResponse toAuthUser(UserAccount user) {
        return new AuthUserResponse(user.id(), user.username(), user.email());
    }
}
