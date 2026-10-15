package com.example.styloflow.enums;

/**
 * User roles. {@code ADMIN}, {@code CASHIER} and {@code STYLIST} belong to a business;
 * {@code SUPERADMIN} is platform-only and must never be assigned to a business user
 * (the {@code users.role} CHECK constraint rejects it).
 */
public enum Role {
    ADMIN, CASHIER, STYLIST, SUPERADMIN
}
