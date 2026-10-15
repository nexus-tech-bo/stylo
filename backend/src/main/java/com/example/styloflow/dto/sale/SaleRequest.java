package com.example.styloflow.dto.sale;

import com.example.styloflow.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param discount       optional global discount, at most the subtotal
 * @param amountReceived optional; for CASH it must cover the total (defaults to the total)
 */
public record SaleRequest(
        Long customerId,
        @NotNull @Size(min = 1, max = 50) List<@Valid @NotNull SaleItemRequest> items,
        @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal discount,
        @NotNull PaymentMethod paymentMethod,
        @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal amountReceived,
        @Size(max = 500) String notes
) {
}
