package ru.itmo.marketplacemock.user;

import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

@Repository
public class MockUserRepository {

    private final List<MockUser> users;

    public MockUserRepository(PasswordEncoder passwordEncoder) {
        this.users = List.of(
                new MockUser(5, "Алиса", "alice", passwordEncoder.encode("alice123")),
                new MockUser(6, "Борис", "boris", passwordEncoder.encode("boris123")),
                new MockUser(7, "Виктория", "victoria", passwordEncoder.encode("victoria123"))
        );
    }

    public Optional<MockUser> findByUsername(String username) {
        return users.stream().filter(user -> user.username().equals(username)).findFirst();
    }
}
