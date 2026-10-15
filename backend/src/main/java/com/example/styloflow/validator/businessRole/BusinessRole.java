package com.example.styloflow.validator.businessRole;

import com.example.styloflow.enums.Role;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/** The role must be assignable to a business user: anything but {@link Role#SUPERADMIN}. {@code null} is valid. */
@Documented
@Constraint(validatedBy = BusinessRole.BusinessRoleValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessRole {
    String message() default "must be ADMIN, CASHIER or STYLIST";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class BusinessRoleValidator implements ConstraintValidator<BusinessRole, Role> {
        @Override
        public boolean isValid(Role value, ConstraintValidatorContext context) {
            return value != Role.SUPERADMIN;
        }
    }
}
