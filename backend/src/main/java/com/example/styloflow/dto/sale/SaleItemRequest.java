package com.example.styloflow.dto.sale;

import com.example.styloflow.enums.ItemType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * @param unitPrice optional override of the catalog price
 * @param discount  optional line discount, at most unit price × quantity
 * @param stylistId optional active stylist who performed the service
 */
public record SaleItemRequest(
        @NotNull ItemType type,
        @NotNull Long itemId,
        @NotNull @Min(1) @Max(999) Integer quantity,
        @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,
        @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal discount,
        Long stylistId
) {
}
