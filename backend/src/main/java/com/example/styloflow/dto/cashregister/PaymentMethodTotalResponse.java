package com.example.styloflow.dto.cashregister;

import com.example.styloflow.enums.PaymentMethod;

import java.math.BigDecimal;

public record PaymentMethodTotalResponse(PaymentMethod method, long count, BigDecimal total) {
}
