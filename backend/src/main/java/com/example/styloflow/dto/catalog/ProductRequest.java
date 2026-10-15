package com.example.styloflow.dto.catalog;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Body of the product PUT (JSON) and form fields of the POST (multipart, plus the {@code image} part).
 * A blank {@code sku} becomes {@code null}; {@code minStock} defaults to 0 and {@code active} to {@code true}.
 * {@code stock} is the initial stock, validated only on create ({@link OnCreate}); the PUT ignores it, stock then
 * changes only through sales and the stock adjustment.
 */
public record ProductRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 50) String sku,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @NotNull(groups = OnCreate.class) @Min(value = 0, groups = OnCreate.class) Integer stock,
        @Min(0) Integer minStock,
        Boolean active
) {
    public ProductRequest {
        sku = (sku == null || sku.isBlank()) ? null : sku.strip();
        if (minStock == null) minStock = 0;
        if (active == null) active = true;
    }

    /** Validation group for the create-only constraints. */
    public interface OnCreate {
    }
}
