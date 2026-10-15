package com.example.styloflow.dto.report;

import java.math.BigDecimal;

/**
 * Services performed by a stylist. {@code commissionRate} is the stylist's current rate; {@code commission}
 * uses the rate snapshot of each sale line.
 */
public record StylistTotalResponse(
        Long stylistId,
        String stylist,
        long services,
        BigDecimal total,
        BigDecimal commissionRate,
        BigDecimal commission
) {
}
