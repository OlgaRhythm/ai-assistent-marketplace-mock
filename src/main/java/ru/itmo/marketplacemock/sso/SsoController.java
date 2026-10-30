package ru.itmo.marketplacemock.sso;

import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.marketplacemock.user.MockUser;
import ru.itmo.marketplacemock.user.MockUserRepository;

@RestController
@RequestMapping("/api")
public class SsoController {

    private final MockUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public SsoController(MockUserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        Optional<MockUser> user = users.findByUsername(request.username());
        if (user.isEmpty() || !passwordEncoder.matches(request.password(), user.get().getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password."));
        }
        return ResponseEntity.ok(Map.of("token", jwtService.issueToken(user.get().getId())));
    }
}
