package com.example.styloflow.dto.sale;

import com.example.styloflow.enums.PaymentMethod;
import com.example.styloflow.enums.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** @param cashRegisterOpen whether the sale's register is still open (the sale can still be voided) */
public record SaleSummaryResponse(
        Long id,
        Instant date,
        String cashier,
        Long customerId,
        String customer,
        BigDecimal total,
        PaymentMethod paymentMethod,
        SaleStatus status,
        boolean cashRegisterOpen
) {
}
