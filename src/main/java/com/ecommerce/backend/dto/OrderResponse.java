package com.ecommerce.backend.dto;

import com.ecommerce.backend.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        OrderStatus status,
        String shippingAddress,
        Instant createdAt
) {
}
