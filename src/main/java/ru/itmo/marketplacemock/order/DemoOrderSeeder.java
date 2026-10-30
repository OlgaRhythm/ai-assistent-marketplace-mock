package ru.itmo.marketplacemock.order;

import java.time.Instant;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoOrderSeeder implements ApplicationRunner {

    private final OrderRepository orders;

    public DemoOrderSeeder(OrderRepository orders) {
        this.orders = orders;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (orders.count() > 0) {
            return;
        }
        Instant now = Instant.now();
        orders.save(new Order(5L, "1 red book", "created", now));
        orders.save(new Order(6L, "white table lamp", "created", now));
        orders.save(new Order(7L, "plates with cat pictures", "delivered", now));
        orders.save(new Order(7L, "pillow", "created", now));
    }
}
