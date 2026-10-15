package com.example.styloflow.dto.business;

import java.math.BigDecimal;

public record BusinessResponse(
        String code,
        String name,
        String taxId,
        String address,
        String phone,
        String currency,
        String currencySymbol,
        BigDecimal taxRate,
        String receiptMessage
) {
}
