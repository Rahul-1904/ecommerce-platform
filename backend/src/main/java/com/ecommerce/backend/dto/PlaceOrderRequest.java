package com.ecommerce.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record PlaceOrderRequest(
        @NotBlank String shippingAddress
) {
}
