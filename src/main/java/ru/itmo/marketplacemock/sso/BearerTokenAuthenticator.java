package ru.itmo.marketplacemock.sso;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class BearerTokenAuthenticator {

    private final JwtService jwtService;

    public BearerTokenAuthenticator(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public Optional<Long> resolveUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = header.substring("Bearer ".length());
        try {
            return Optional.of(jwtService.verifyAndGetUserId(token));
        } catch (JwtException | NumberFormatException e) {
            return Optional.empty();
        }
    }
}
