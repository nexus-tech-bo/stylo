package com.example.styloflow.dto.catalog;

import java.math.BigDecimal;

/** {@code category} is the category name. */
public record SalonServiceResponse(
        Long id,
        Long categoryId,
        String category,
        String name,
        String description,
        Integer durationMinutes,
        BigDecimal price,
        boolean active,
        String imageUrl
) {
}
