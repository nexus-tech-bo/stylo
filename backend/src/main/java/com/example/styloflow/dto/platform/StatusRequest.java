package com.example.styloflow.dto.platform;

import jakarta.validation.constraints.NotNull;

/** @param active {@code false} suspends the business, {@code true} reactivates it */
public record StatusRequest(@NotNull Boolean active) {
}
