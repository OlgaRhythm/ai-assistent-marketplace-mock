package ru.itmo.marketplacemock.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String orderComposition;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant deliveryDate;

    protected Order() {
    }

    public Order(Long userId, String orderComposition, String status, Instant deliveryDate) {
        this.userId = userId;
        this.orderComposition = orderComposition;
        this.status = status;
        this.deliveryDate = deliveryDate;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getOrderComposition() {
        return orderComposition;
    }

    public String getStatus() {
        return status;
    }

    public Instant getDeliveryDate() {
        return deliveryDate;
    }
}
