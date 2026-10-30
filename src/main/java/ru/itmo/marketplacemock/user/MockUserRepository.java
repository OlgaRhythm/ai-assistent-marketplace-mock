package ru.itmo.marketplacemock.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockUserRepository extends JpaRepository<MockUser, Long> {

    Optional<MockUser> findByUsername(String username);
}
