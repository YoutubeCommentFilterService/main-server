package com.hanhome.youtube_comments.oauth.config;

import com.hanhome.youtube_comments.utils.AESUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class AESConfig {
    @Value("${spring.security.cipher.secret-key}")
    private String secretKey;

    @Bean
    public SecretKeySpec aesSecretKey() {
        return new SecretKeySpec(secretKey.getBytes(), "AES");
    }

    @Bean
    public AESUtil aesUtil(SecretKeySpec secretKey) {
        return new AESUtil(secretKey);
    }
}
