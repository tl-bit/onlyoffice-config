package com.example.onlyoffice.service;

import com.example.onlyoffice.config.OnlyOfficeProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 服务类
 * 
 * 负责 JWT Token 的生成和验证
 * 用于 ONLYOFFICE 与后端之间的安全通信
 * 
 * 注意：使用手动实现以支持任意长度的密钥，与 ONLYOFFICE 保持兼容
 * 
 * @author Your Name
 * @version 1.0.0
 */
@Slf4j
@Service
public class JwtService {

    private final OnlyOfficeProperties properties;
    private final ObjectMapper objectMapper;
    private byte[] secretKeyBytes;

    public JwtService(OnlyOfficeProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 初始化密钥
     */
    @PostConstruct
    public void init() {
        String secret = properties.getJwt().getSecret();
        this.secretKeyBytes = secret.getBytes(StandardCharsets.UTF_8);
        log.info("JWT 服务初始化完成");
    }

    /**
     * 为对象生成 JWT Token
     * 
     * @param payload 要签名的对象
     * @return JWT Token 字符串
     */
    public String createToken(Object payload) {
        try {
            // 将对象转换为 Map
            String json = objectMapper.writeValueAsString(payload);
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(json, Map.class);
            
            return createToken(claims);
        } catch (JsonProcessingException e) {
            log.error("创建 JWT Token 失败: {}", e.getMessage());
            throw new RuntimeException("创建 JWT Token 失败", e);
        }
    }

    /**
     * 为 Map 生成 JWT Token（手动实现，支持任意长度密钥）
     * 
     * @param claims 要签名的数据
     * @return JWT Token 字符串
     */
    public String createToken(Map<String, Object> claims) {
        try {
            // 添加 iat 和 exp
            long now = System.currentTimeMillis() / 1000;
            long exp = now + properties.getJwt().getExpiresIn();
            
            Map<String, Object> fullClaims = new HashMap<>(claims);
            fullClaims.put("iat", now);
            fullClaims.put("exp", exp);
            
            // Header
            Map<String, String> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            
            // Base64Url 编码
            String headerJson = objectMapper.writeValueAsString(header);
            String payloadJson = objectMapper.writeValueAsString(fullClaims);
            
            String headerBase64 = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadBase64 = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
            
            // 签名
            String dataToSign = headerBase64 + "." + payloadBase64;
            byte[] signature = hmacSha256(dataToSign.getBytes(StandardCharsets.UTF_8));
            String signatureBase64 = base64UrlEncode(signature);
            
            return dataToSign + "." + signatureBase64;
        } catch (Exception e) {
            log.error("创建 JWT Token 失败: {}", e.getMessage());
            throw new RuntimeException("创建 JWT Token 失败", e);
        }
    }

    /**
     * 验证 JWT Token
     * 
     * @param token JWT Token 字符串
     * @return 解析后的 Claims
     */
    public Map<String, Object> verifyToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new RuntimeException("Invalid JWT format");
            }
            
            // 验证签名
            String dataToSign = parts[0] + "." + parts[1];
            byte[] expectedSignature = hmacSha256(dataToSign.getBytes(StandardCharsets.UTF_8));
            String expectedSignatureBase64 = base64UrlEncode(expectedSignature);
            
            if (!expectedSignatureBase64.equals(parts[2])) {
                throw new RuntimeException("JWT signature verification failed");
            }
            
            // 解析 payload
            String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);
            
            // 检查过期时间
            if (claims.containsKey("exp")) {
                long exp = ((Number) claims.get("exp")).longValue();
                if (System.currentTimeMillis() / 1000 > exp) {
                    throw new RuntimeException("JWT token expired");
                }
            }
            
            return claims;
        } catch (Exception e) {
            log.warn("JWT Token 验证失败: {}", e.getMessage());
            throw new RuntimeException("JWT Token 验证失败", e);
        }
    }

    /**
     * 验证 Token 是否有效
     * 
     * @param token JWT Token 字符串
     * @return true 如果有效
     */
    public boolean isTokenValid(String token) {
        try {
            verifyToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 Token 中提取指定字段
     * 
     * @param token JWT Token 字符串
     * @param key 字段名
     * @return 字段值
     */
    public Object getClaimValue(String token, String key) {
        Map<String, Object> claims = verifyToken(token);
        return claims.get(key);
    }
    
    /**
     * HMAC-SHA256 签名
     */
    private byte[] hmacSha256(byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secretKeyBytes, "HmacSHA256");
        mac.init(keySpec);
        return mac.doFinal(data);
    }
    
    /**
     * Base64Url 编码（无填充）
     */
    private String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
    
    /**
     * Base64Url 解码
     */
    private byte[] base64UrlDecode(String data) {
        return Base64.getUrlDecoder().decode(data);
    }
}
