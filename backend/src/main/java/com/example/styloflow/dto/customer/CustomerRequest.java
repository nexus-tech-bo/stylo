package com.example.styloflow.dto.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 30) String phone,
        @Email @Size(max = 120) String email,
        @Size(max = 30) String taxId,
        @Size(max = 500) String notes
) {
}
