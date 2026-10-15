package com.example.styloflow.security;

import com.example.styloflow.enums.Role;

/**
 * Principal of every authenticated request; inject it with {@code @AuthenticationPrincipal AuthenticatedUser user}.
 *
 * @param id         user id (business token) or platform admin id (platform token)
 * @param businessId business of the token; {@code null} for {@link Role#SUPERADMIN}
 * @param role       role (business users: reloaded from the database on every request)
 * @param name       display name (business users: reloaded from the database on every request)
 */
public record AuthenticatedUser(Long id, Long businessId, Role role, String name) {
}
