package ru.itmo.marketplacemock.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoUserSeeder implements ApplicationRunner {

    private final MockUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DemoUserSeeder(MockUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        users.save(new MockUser(5L, "Алиса", "alice", passwordEncoder.encode("alice123")));
        users.save(new MockUser(6L, "Борис", "boris", passwordEncoder.encode("boris123")));
        users.save(new MockUser(7L, "Виктория", "victoria", passwordEncoder.encode("victoria123")));
    }
}
