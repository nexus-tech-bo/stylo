package com.example.styloflow.dto.cashregister;

import com.example.styloflow.enums.CashRegisterStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * A cash-register session with its totals (completed sales only).
 *
 * @param expectedCash opening amount + cash sales (live while open, snapshot once closed)
 * @param difference   counted − expected; {@code null} while open
 */
public record CashRegisterResponse(
        Long id,
        CashRegisterStatus status,
        String openedBy,
        Instant openedAt,
        BigDecimal openingAmount,
        String closedBy,
        Instant closedAt,
        long salesCount,
        BigDecimal salesTotal,
        BigDecimal expectedCash,
        BigDecimal countedCash,
        BigDecimal difference,
        List<PaymentMethodTotalResponse> byPaymentMethod,
        String notes
) {
}
