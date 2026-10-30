package ru.itmo.marketplacemock.order;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByIdAsc(Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);
}
