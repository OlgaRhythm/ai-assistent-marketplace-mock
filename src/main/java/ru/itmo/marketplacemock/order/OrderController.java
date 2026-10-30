package ru.itmo.marketplacemock.order;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.marketplacemock.sso.BearerTokenAuthenticator;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orders;
    private final BearerTokenAuthenticator auth;

    public OrderController(OrderRepository orders, BearerTokenAuthenticator auth) {
        this.orders = orders;
        this.auth = auth;
    }

    @GetMapping
    public ResponseEntity<?> listOrders(HttpServletRequest request) {
        Optional<Long> userId = auth.resolveUserId(request);
        if (userId.isEmpty()) {
            return unauthorized();
        }
        List<OrderDto> result = orders.findByUserIdOrderByIdAsc(userId.get()).stream()
                .map(OrderDto::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable long id, HttpServletRequest request) {
        Optional<Long> userId = auth.resolveUserId(request);
        if (userId.isEmpty()) {
            return unauthorized();
        }
        return orders.findByIdAndUserId(id, userId.get())
                .map(order -> ResponseEntity.ok(OrderDto.from(order)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token."));
    }
}
