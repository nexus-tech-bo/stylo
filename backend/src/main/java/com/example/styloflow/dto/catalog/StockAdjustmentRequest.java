package com.example.styloflow.dto.catalog;

import jakarta.validation.constraints.NotNull;

/** Positive {@code quantity} adds units, negative removes them. */
public record StockAdjustmentRequest(@NotNull Integer quantity) {
}
