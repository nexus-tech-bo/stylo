package com.example.styloflow.dto.catalog;

import java.math.BigDecimal;

/** {@code lowStock} is {@code stock <= minStock}. */
public record ProductResponse(
        Long id,
        String name,
        String sku,
        BigDecimal price,
        Integer stock,
        Integer minStock,
        boolean active,
        boolean lowStock,
        String imageUrl
) {
}
