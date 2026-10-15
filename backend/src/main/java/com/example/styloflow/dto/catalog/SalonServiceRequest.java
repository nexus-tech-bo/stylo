package com.example.styloflow.dto.catalog;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Body of the service PUT (JSON) and form fields of the POST (multipart, plus the {@code image} part).
 * {@code active} defaults to {@code true} when omitted.
 */
public record SalonServiceRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotNull @Min(1) Integer durationMinutes,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
        Boolean active
) {
    public SalonServiceRequest {
        if (active == null) active = true;
    }
}
