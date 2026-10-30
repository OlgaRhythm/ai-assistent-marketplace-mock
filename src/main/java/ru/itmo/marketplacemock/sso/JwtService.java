package ru.itmo.marketplacemock.sso;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
import ru.itmo.marketplacemock.config.SsoProperties;

@Service
public class JwtService {

    private final SecretKey key;
    private final SsoProperties properties;

    public JwtService(SsoProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSharedSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueToken(long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getTokenTtl())))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public long verifyAndGetUserId(String token) {
        return Long.parseLong(
                Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getSubject());
    }
}
