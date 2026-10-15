package com.example.styloflow.dto.user;

import com.example.styloflow.enums.Role;
import com.example.styloflow.validator.businessRole.BusinessRole;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Create or update a business user.
 *
 * @param password required on create (422 otherwise); on update {@code null} keeps the current one
 * @param commissionRate {@code null} = 0 on create, unchanged on update
 * @param active {@code null} = active on create, unchanged on update
 */
public record UserRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "only letters, numbers, '.', '_' and '-'")
        String username,
        @Size(min = 6, max = 72) String password,
        @NotNull @BusinessRole Role role,
        @Size(max = 30) String phone,
        @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal commissionRate,
        Boolean active
) {
}
