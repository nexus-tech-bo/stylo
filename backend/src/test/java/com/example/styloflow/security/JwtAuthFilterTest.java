package com.example.styloflow.security;

import com.example.styloflow.config.tenant.TenantContext;
import com.example.styloflow.entity.Business;
import com.example.styloflow.entity.User;
import com.example.styloflow.enums.Role;
import com.example.styloflow.repository.BusinessRepository;
import com.example.styloflow.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {
    private static final AuthenticatedUser TOKEN_ADMIN = new AuthenticatedUser(7L, 1L, Role.ADMIN, "Old name");

    @Mock
    JwtService jwtService;
    @Mock
    BusinessRepository businessRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    ProblemResponseWriter problemWriter;
    @InjectMocks
    JwtAuthFilter filter;

    private Authentication authSeenByChain;
    private String tenantSeenByChain;

    @BeforeEach
    void tokenForActiveBusiness() {
        when(jwtService.parse("t")).thenReturn(Optional.of(TOKEN_ADMIN));
        when(businessRepository.findById(1L))
                .thenReturn(Optional.of(Business.builder().id(1L).schemaName("business_1").build()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void doFilter() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/users");
        request.addHeader("Authorization", "Bearer t");
        filter.doFilter(request, new MockHttpServletResponse(), (_, _) -> {
            authSeenByChain = SecurityContextHolder.getContext().getAuthentication();
            tenantSeenByChain = TenantContext.getCurrentTenant();
        });
    }

    private static User user(Role role, boolean active) {
        return User.builder().id(7L).name("New name").username("ana").role(role).active(active).build();
    }

    @Test
    void doFilter_should_useRoleAndNameFromDatabase_when_roleChanged() throws Exception {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(Role.CASHIER, true)));

        doFilter();

        assertThat(authSeenByChain.getPrincipal()).isEqualTo(new AuthenticatedUser(7L, 1L, Role.CASHIER, "New name"));
        assertThat(authSeenByChain.getAuthorities()).extracting(a -> a.getAuthority()).containsExactly("ROLE_CASHIER");
        assertThat(tenantSeenByChain).isEqualTo("business_1");
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    void doFilter_should_leaveUnauthenticated_when_userInactive() throws Exception {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(Role.ADMIN, false)));

        doFilter();

        assertThat(authSeenByChain).isNull();
        assertThat(tenantSeenByChain).isNull();
    }

    @Test
    void doFilter_should_leaveUnauthenticated_when_userDeleted() throws Exception {
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        doFilter();

        assertThat(authSeenByChain).isNull();
        assertThat(tenantSeenByChain).isNull();
    }
}
