package ru.itmo.marketplacemock.order;

import java.time.Instant;

public record OrderDto(long id, String orderComposition, String status, Instant deliveryDate) {

    public static OrderDto from(Order order) {
        return new OrderDto(order.getId(), order.getOrderComposition(), order.getStatus(), order.getDeliveryDate());
    }
}
