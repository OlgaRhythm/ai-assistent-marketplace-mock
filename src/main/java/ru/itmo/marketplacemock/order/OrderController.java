package ru.itmo.marketplacemock.order;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.marketplacemock.sso.BearerTokenAuthenticator;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

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
        log.info(Markers.appendEntries(Map.of("action", "orders_list", "authz_result", "allowed", "user_id", userId.get())),
                "orders listed");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable long id, HttpServletRequest request) {
        Optional<Long> userId = auth.resolveUserId(request);
        if (userId.isEmpty()) {
            return unauthorized();
        }
        Optional<Order> order = orders.findByIdAndUserId(id, userId.get());
        log.info(Markers.appendEntries(Map.of(
                        "action", "order_get",
                        "authz_result", order.isPresent() ? "allowed" : "denied",
                        "user_id", userId.get())),
                "order lookup");
        return order
                .map(found -> ResponseEntity.ok(OrderDto.from(found)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token."));
    }
}
