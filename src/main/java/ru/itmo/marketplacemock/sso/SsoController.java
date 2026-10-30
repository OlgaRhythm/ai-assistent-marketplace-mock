package ru.itmo.marketplacemock.sso;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(SsoController.class);

    private final MockUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String dummyHash;

    public SsoController(MockUserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        Optional<MockUser> user = users.findByUsername(request.username());
        String hashToCheck = user.map(MockUser::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);
        if (user.isEmpty() || !passwordMatches) {
            log.warn(Markers.appendEntries(Map.of("action", "sso_login", "authz_result", "denied")),
                    "login rejected");
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password."));
        }
        log.info(Markers.appendEntries(Map.of("action", "sso_login", "authz_result", "allowed", "user_id", user.get().getId())),
                "login ok");
        return ResponseEntity.ok(Map.of("token", jwtService.issueToken(user.get().getId())));
    }
}
