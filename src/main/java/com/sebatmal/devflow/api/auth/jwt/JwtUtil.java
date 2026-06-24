package com.sebatmal.devflow.api.auth.jwt;

import com.sebatmal.devflow.common.exception.DevflowException;
import com.sebatmal.devflow.enums.message.FailMessage;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessExpirationMs;

    public JwtUtil(
            @Value("${jwt.secret:devflow-local-development-secret-key-change-me-please-0123456789}") final String secret,
            @Value("${jwt.access-expiration-ms:86400000}") final long accessExpirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
    }

    public String createAccessToken(final Long userId) {
        final Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessExpirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long getUserId(final String token) {
        try {
            final Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Long.valueOf(claims.getSubject());
        } catch (final ExpiredJwtException e) {
            throw new DevflowException(FailMessage.UNAUTHORIZED_EXPIRED, e);
        } catch (final JwtException | IllegalArgumentException e) {
            throw new DevflowException(FailMessage.UNAUTHORIZED_INVALID_TOKEN, e);
        }
    }
}
