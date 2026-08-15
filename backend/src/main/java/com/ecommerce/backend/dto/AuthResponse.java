package com.ecommerce.backend.dto;

import com.ecommerce.backend.entity.Role;

public record AuthResponse(
        String token,
        String email,
        Role role
) {
}
