package com.example.musescoreback.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final long expireSeconds;

    public JwtService(@Value("${app.auth.secret}") String secret,
                      @Value("${app.auth.expire-seconds}") long expireSeconds) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expireSeconds = expireSeconds;
    }

    public String generateToken(String userId) {
        try {
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", userId);
            payload.put("exp", Instant.now().getEpochSecond() + expireSeconds);

            String headerPart = URL_ENCODER.encodeToString(OBJECT_MAPPER.writeValueAsBytes(header));
            String payloadPart = URL_ENCODER.encodeToString(OBJECT_MAPPER.writeValueAsBytes(payload));
            String content = headerPart + "." + payloadPart;
            String signature = sign(content);
            return content + "." + signature;
        } catch (Exception ex) {
            throw new IllegalStateException("生成令牌失败", ex);
        }
    }

    public Optional<String> parseUserId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }

            String content = parts[0] + "." + parts[1];
            String expectedSign = sign(content);
            if (!MessageDigest.isEqual(expectedSign.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
                return Optional.empty();
            }

            byte[] payloadRaw = URL_DECODER.decode(parts[1]);
            Map<String, Object> payload = OBJECT_MAPPER.readValue(payloadRaw, new TypeReference<>() {
            });
            Object expObj = payload.get("exp");
            if (expObj == null) {
                return Optional.empty();
            }

            long exp = Long.parseLong(String.valueOf(expObj));
            if (exp <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }

            Object sub = payload.get("sub");
            if (sub == null) {
                return Optional.empty();
            }
            return Optional.of(String.valueOf(sub));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        byte[] sign = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return URL_ENCODER.encodeToString(sign);
    }
}
