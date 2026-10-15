package com.example.styloflow.dto.platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Creates a business and its first ADMIN user.
 *
 * @param baseCatalog {@code true} seeds a few categories, services and products; {@code null} = {@code false}
 */
public record CreateBusinessRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(min = 3, max = 40) @Pattern(regexp = "^[a-z0-9-]*$", message = "only lowercase letters, numbers and '-'")
        String code,
        @Size(max = 30) String taxId,
        @Size(max = 30) String phone,
        @NotBlank @Size(max = 120) String adminName,
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "only letters, numbers, '.', '_' and '-'")
        String adminUsername,
        @NotBlank @Size(min = 6, max = 72) String adminPassword,
        Boolean baseCatalog
) {
}
