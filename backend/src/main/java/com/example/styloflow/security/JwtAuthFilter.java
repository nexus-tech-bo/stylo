package com.example.styloflow.security;

import com.example.styloflow.config.tenant.TenantContext;
import com.example.styloflow.entity.User;
import com.example.styloflow.repository.BusinessRepository;
import com.example.styloflow.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Bearer token to {@link AuthenticatedUser}. For business tokens it also checks that the business exists and is
 * active (403 "Business suspended" otherwise), sets {@link TenantContext} for the whole request and reloads the user
 * by id: a deleted or inactive user is unauthenticated, and role and name come from the database, not the claims,
 * so a deactivation or role change applies on the next request. An invalid token leaves the request
 * unauthenticated, so protected paths answer 401.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Set<String> LOGIN_PATHS = Set.of("/api/auth/login", "/api/platform/auth/login");

    private final JwtService jwtService;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final ProblemResponseWriter problemWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return LOGIN_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            var header = request.getHeader("Authorization");
            var principal = header != null && header.startsWith("Bearer ")
                    ? jwtService.parse(header.substring(7)).orElse(null)
                    : null;
            if (principal != null && principal.businessId() != null) {
                var business = businessRepository.findById(principal.businessId()).orElse(null);
                if (business == null) {
                    principal = null; // business deleted: token no longer valid
                } else if (!business.isActive()) {
                    problemWriter.write(response, HttpStatus.FORBIDDEN, "Business suspended", "This business is suspended");
                    return;
                } else {
                    TenantContext.setCurrentTenant(business.getSchemaName());
                    var businessId = principal.businessId();
                    // One PK lookup per request, resolved in the tenant schema just set.
                    principal = userRepository.findById(principal.id())
                            .filter(User::isActive)
                            .map(u -> new AuthenticatedUser(u.getId(), businessId, u.getRole(), u.getName()))
                            .orElse(null);
                    if (principal == null) TenantContext.clear();
                }
            }
            if (principal != null) {
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, null, authorities));
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
