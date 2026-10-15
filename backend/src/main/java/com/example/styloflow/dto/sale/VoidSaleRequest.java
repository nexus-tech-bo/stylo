package com.example.styloflow.dto.sale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VoidSaleRequest(@NotBlank @Size(max = 250) String reason) {
}
