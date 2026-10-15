package com.example.styloflow.dto.user;

import com.example.styloflow.enums.Role;

import java.math.BigDecimal;

public record UserResponse(
        Long id,
        String name,
        String username,
        Role role,
        String phone,
        BigDecimal commissionRate,
        boolean active
) {
}
