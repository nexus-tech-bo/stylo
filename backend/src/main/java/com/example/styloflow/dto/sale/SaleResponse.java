package com.example.styloflow.dto.sale;

import com.example.styloflow.enums.PaymentMethod;
import com.example.styloflow.enums.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * @param subtotal sum of the items' net subtotals
 * @param discount global discount only
 * @param tax      tax included in {@code total}
 */
public record SaleResponse(
        Long id,
        Instant date,
        Long cashRegisterId,
        String cashier,
        Long customerId,
        String customer,
        String customerTaxId,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        BigDecimal tax,
        PaymentMethod paymentMethod,
        BigDecimal amountReceived,
        BigDecimal change,
        SaleStatus status,
        String voidedBy,
        Instant voidedAt,
        String voidReason,
        String notes,
        List<SaleItemResponse> items
) {
}
