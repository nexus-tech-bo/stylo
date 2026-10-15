package com.example.styloflow.support;

import com.example.styloflow.enums.Role;
import com.example.styloflow.security.AuthenticatedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/** MockMvc authentication with the same principal the JWT filter builds: {@code .with(TestAuth.as(1L, Role.ADMIN))}. */
public final class TestAuth {
    public static final Long BUSINESS_ID = 1L;

    private TestAuth() {
    }

    public static RequestPostProcessor as(Long userId, Role role) {
        var businessId = role == Role.SUPERADMIN ? null : BUSINESS_ID;
        var principal = new AuthenticatedUser(userId, businessId, role, "Test " + role);
        return authentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }
}
