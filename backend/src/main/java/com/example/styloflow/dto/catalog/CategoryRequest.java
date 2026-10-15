package com.example.styloflow.dto.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code active} defaults to {@code true} when omitted. */
public record CategoryRequest(
        @NotBlank @Size(max = 80) String name,
        Boolean active
) {
    public CategoryRequest {
        if (active == null) active = true;
    }
}
