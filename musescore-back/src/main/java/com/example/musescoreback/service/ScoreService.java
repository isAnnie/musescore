package com.example.musescoreback.service;

import com.example.musescoreback.dto.ScoreResponse;
import com.example.musescoreback.dto.ScoreUpsertRequest;
import com.example.musescoreback.exception.BizException;
import com.example.musescoreback.repository.ScoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ScoreService {

    private final ScoreRepository scoreRepository;
    private final JwtService jwtService;

    public ScoreService(ScoreRepository scoreRepository, JwtService jwtService) {
        this.scoreRepository = scoreRepository;
        this.jwtService = jwtService;
    }

    public List<ScoreResponse> myScores(String bearerToken) {
        String userId = resolveUserId(bearerToken);
        return scoreRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ScoreResponse getById(String bearerToken, String id) {
        String userId = resolveUserId(bearerToken);
        return scoreRepository.findByIdAndUserId(id, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new BizException(HttpStatus.NOT_FOUND, "乐谱不存在"));
    }

    public ScoreResponse create(String bearerToken, ScoreUpsertRequest request) {
        String userId = resolveUserId(bearerToken);
        validateRequest(request);
        return toResponse(scoreRepository.insert(userId, request));
    }

    public ScoreResponse update(String bearerToken, String id, ScoreUpsertRequest request) {
        String userId = resolveUserId(bearerToken);
        validateRequest(request);
        return scoreRepository.update(id, userId, request)
                .map(this::toResponse)
                .orElseThrow(() -> new BizException(HttpStatus.NOT_FOUND, "乐谱不存在"));
    }

    public void delete(String bearerToken, String id) {
        String userId = resolveUserId(bearerToken);
        boolean deleted = scoreRepository.delete(id, userId);
        if (!deleted) {
            throw new BizException(HttpStatus.NOT_FOUND, "乐谱不存在");
        }
    }

    private void validateRequest(ScoreUpsertRequest request) {
        if (request == null || !StringUtils.hasText(request.title())) {
            throw new BizException(HttpStatus.BAD_REQUEST, "乐谱标题不能为空");
        }
    }

    private String resolveUserId(String bearerToken) {
        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) {
            throw new BizException(HttpStatus.UNAUTHORIZED, "缺少认证令牌");
        }
        String token = bearerToken.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            throw new BizException(HttpStatus.UNAUTHORIZED, "缺少认证令牌");
        }
        return jwtService.parseUserId(token)
                .orElseThrow(() -> new BizException(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录"));
    }

    private ScoreResponse toResponse(ScoreRepository.ScoreRow row) {
        return new ScoreResponse(
                row.id(),
                row.title(),
                row.composer(),
                row.tempo(),
                row.timeSignature(),
                row.keySignature(),
                row.notes(),
                row.measures(),
                row.createdAt(),
                row.updatedAt(),
                row.isDraft(),
                row.tags(),
                row.description(),
                row.visibility()
        );
    }
}
