package com.touhouqing.grabteacherbackend.config;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.util.Base64;

@Configuration
public class JwtConfiguration {

    /**
     * JWT密钥，从环境变量读取，如果环境变量不存在则使用默认值
     * 环境变量名：JWT_SECRET
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * JWT过期时间（毫秒），从配置文件读取
     */
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Bean
    public SecretKey jwtSecretKey() {
        // 自动生成符合HS512要求的安全密钥
        return Keys.secretKeyFor(SignatureAlgorithm.HS512);
    }

    @Bean
    public String jwtSecretString() {
        // 如果需要字符串形式的密钥
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
}