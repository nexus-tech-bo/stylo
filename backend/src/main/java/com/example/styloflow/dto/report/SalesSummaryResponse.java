package com.example.styloflow.dto.report;

import com.example.styloflow.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Totals of the completed sales in a range; {@code voidedSales} counts the voided ones.
 * {@code totalDiscounts} includes global and line discounts.
 */
public record SalesSummaryResponse(
        LocalDate from,
        LocalDate to,
        long salesCount,
        BigDecimal salesTotal,
        BigDecimal averageTicket,
        BigDecimal totalDiscounts,
        BigDecimal totalTax,
        long voidedSales,
        List<PaymentMethodTotal> byPaymentMethod
) {
    public record PaymentMethodTotal(PaymentMethod method, long count, BigDecimal total) {
    }
}
