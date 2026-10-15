package com.example.styloflow.dto.cashregister;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CloseCashRegisterRequest(
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal countedCash,
        @Size(max = 500) String notes
) {
}
