package com.example.styloflow.dto.cashregister;

import com.example.styloflow.enums.PaymentMethod;

import java.math.BigDecimal;

/**
 * Query projection: completed sales of one cash register grouped by payment method.
 *
 * @param cashRegisterId cash register
 * @param method         payment method
 * @param count          number of completed sales
 * @param total          sum of their totals
 */
public record CashRegisterPaymentTotal(Long cashRegisterId, PaymentMethod method, Long count, BigDecimal total) {
}
