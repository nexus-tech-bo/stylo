package com.example.styloflow.dto.sale;

import com.example.styloflow.enums.ItemType;

import java.math.BigDecimal;

public record SaleItemResponse(
        Long id,
        ItemType type,
        Long itemId,
        String description,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal discount,
        BigDecimal subtotal,
        Long stylistId,
        String stylist
) {
}
