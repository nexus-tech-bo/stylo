package com.example.styloflow.dto.business;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Locale;

/** Editable settings of the business of the token; {@code code} is not editable. */
public record BusinessRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 30) String taxId,
        @Size(max = 200) String address,
        @Size(max = 30) String phone,
        @NotNull @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be exactly 3 letters") String currency,
        @NotBlank @Size(max = 5) String currencySymbol,
        @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal taxRate,
        @Size(max = 250) String receiptMessage
) {
    public BusinessRequest {
        if (currency != null) currency = currency.toUpperCase(Locale.ROOT);
    }
}
