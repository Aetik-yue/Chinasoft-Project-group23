package com.chinasoft.smokesensor.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * API Key 加密工具：AES-256-GCM 加密存储，前端脱敏显示。
 *
 * <p>加密后的密文以 {@code ENC:} 前缀标识，便于区分明文与密文（向后兼容）。
 * 加密密钥由配置项 {@code app.api-key-secret} 经 SHA-256 派生。
 */
@Slf4j
@Component
public class ApiKeyEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;
    private static final String PREFIX = "ENC:";

    private final byte[] keyBytes;

    public ApiKeyEncryptor(
            @Value("${app.api-key-secret:parrot-care-default-secret-2026}") String secret) {
        try {
            this.keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("无法初始化 API Key 加密器", e);
        }
    }

    /**
     * 加密明文 API Key，返回 {@code ENC:<base64>} 格式。
     * 空值或空白字符串直接返回原值。
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            SecureRandom.getInstanceStrong().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + ciphertext 拼接后 Base64 编码
            byte[] combined = new byte[IV_BYTES + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_BYTES);
            System.arraycopy(encrypted, 0, combined, IV_BYTES, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("API Key 加密失败", e);
            throw new IllegalStateException("加密失败", e);
        }
    }

    /**
     * 解密 {@code ENC:} 前缀的密文。非 ENC 前缀的值视为明文直接返回（向后兼容）。
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) {
            return cipherText;
        }
        if (!cipherText.startsWith(PREFIX)) {
            // 兼容未加密的旧数据
            return cipherText;
        }
        try {
            byte[] combined = Base64.getDecoder()
                    .decode(cipherText.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_BYTES);
            byte[] encrypted = Arrays.copyOfRange(combined, IV_BYTES, combined.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("API Key 解密失败（密文可能已损坏或密钥不匹配）", e);
            return null;
        }
    }

    /**
     * 脱敏显示：只保留前 3 位和后 4 位，中间用 **** 替代。
     * 长度不足 8 字符时全部遮盖。
     */
    public String mask(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return "";
        }
        if (plainText.length() <= 8) {
            return "****";
        }
        return plainText.substring(0, 3) + "****" + plainText.substring(plainText.length() - 4);
    }
}
