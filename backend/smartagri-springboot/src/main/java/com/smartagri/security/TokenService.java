package com.smartagri.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long DEFAULT_EXPIRATION_MILLIS = 7L * 24 * 60 * 60 * 1000; // 7 days

    private final byte[] secretKeyBytes;
    private final ObjectMapper objectMapper;

    public TokenService(@Value("${app.jwt.secret:smartagri-secure-jwt-token-signing-key-production-strength-2026!}") String secret,
                        ObjectMapper objectMapper) {
        this.secretKeyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    public String generateToken(Long userId, String username, String role) {
        try {
            long now = System.currentTimeMillis();
            long exp = now + DEFAULT_EXPIRATION_MILLIS;

            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userId);
            claims.put("username", username);
            claims.put("role", role);
            claims.put("iat", now);
            claims.put("exp", exp);

            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsString(header).getBytes(StandardCharsets.UTF_8));
            String encodedPayload = base64UrlEncode(objectMapper.writeValueAsString(claims).getBytes(StandardCharsets.UTF_8));

            String dataToSign = encodedHeader + "." + encodedPayload;
            String signature = sign(dataToSign);

            return dataToSign + "." + signature;
        } catch (Exception e) {
            log.error("Failed to generate token", e);
            throw new IllegalStateException("Could not generate authentication token", e);
        }
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        try {
            String dataToSign = parts[0] + "." + parts[1];
            String expectedSignature = sign(dataToSign);

            if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                return false;
            }

            Map<String, Object> claims = parseClaims(parts[1]);
            Number exp = (Number) claims.get("exp");
            if (exp == null || System.currentTimeMillis() > exp.longValue()) {
                return false;
            }

            return true;
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getClaims(String token) {
        if (!validateToken(token)) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            return parseClaims(parts[1]);
        } catch (Exception e) {
            return null;
        }
    }

    public Long getUserId(String token) {
        Map<String, Object> claims = getClaims(token);
        if (claims == null || !claims.containsKey("userId")) {
            return null;
        }
        Number id = (Number) claims.get("userId");
        return id != null ? id.longValue() : null;
    }

    public String getUsername(String token) {
        Map<String, Object> claims = getClaims(token);
        return claims != null ? (String) claims.get("username") : null;
    }

    public String getRole(String token) {
        Map<String, Object> claims = getClaims(token);
        return claims != null ? (String) claims.get("role") : null;
    }

    private String sign(String data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(secretKeyBytes, HMAC_ALGORITHM);
        mac.init(keySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(rawHmac);
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseClaims(String base64UrlPayload) throws Exception {
        byte[] decoded = Base64.getUrlDecoder().decode(base64UrlPayload);
        return objectMapper.readValue(decoded, Map.class);
    }
}
