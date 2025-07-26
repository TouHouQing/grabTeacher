package com.touhouqing.grabteacherbackend.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.touhouqing.grabteacherbackend.security.UserPrincipal;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpirationInMs;

    // 可选：使用自动生成的密钥
    // @Autowired
    // private SecretKey jwtSecretKey;

    private SecretKey getSigningKey() {
        // 方式1：使用配置文件中的密钥（已修复长度问题）
        String paddedSecret = padSecret(jwtSecret);
        return Keys.hmacShaKeyFor(paddedSecret.getBytes(StandardCharsets.UTF_8));
        
        // 方式2：使用自动生成的密钥（取消注释上面的@Autowired）
        // return jwtSecretKey;
    }

    private String padSecret(String secret) {
        // HS512需要至少64字节的密钥
        StringBuilder sb = new StringBuilder(secret);
        while (sb.length() < 64) {
            sb.append(secret);
        }
        return sb.substring(0, 64); // 截取到64字节
    }

    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(Long.toString(userPrincipal.getId()))
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateTokenFromUserId(Long userId) {
        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(Long.toString(userId))
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public Long getUserIdFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(authToken);
            return true;
        } catch (SecurityException ex) {
            System.err.println("JWT签名无效");
        } catch (MalformedJwtException ex) {
            System.err.println("JWT格式错误");
        } catch (ExpiredJwtException ex) {
            System.err.println("JWT已过期");
        } catch (UnsupportedJwtException ex) {
            System.err.println("不支持的JWT");
        } catch (IllegalArgumentException ex) {
            System.err.println("JWT为空");
        }
        return false;
    }
} 