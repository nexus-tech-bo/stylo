package com.example.styloflow.service.impl;

import com.example.styloflow.config.tenant.TenantExecutor;
import com.example.styloflow.dto.auth.LoginRequest;
import com.example.styloflow.dto.auth.LoginResponse;
import com.example.styloflow.dto.auth.PlatformLoginRequest;
import com.example.styloflow.dto.auth.PlatformLoginResponse;
import com.example.styloflow.dto.user.UserResponse;
import com.example.styloflow.exception.BusinessSuspendedException;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.exception.UnauthorizedException;
import com.example.styloflow.mapper.UserMapper;
import com.example.styloflow.repository.BusinessRepository;
import com.example.styloflow.repository.PlatformAdminRepository;
import com.example.styloflow.repository.UserRepository;
import com.example.styloflow.security.JwtService;
import com.example.styloflow.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * No class-level transaction: business login reads {@code public} first and then the tenant schema,
 * which needs two separate transactions (see {@link TenantExecutor}).
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final String INVALID_CREDENTIALS = "Invalid credentials";

    private final BusinessRepository businessRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TenantExecutor tenantExecutor;
    private final UserMapper userMapper;

    @Override
    public LoginResponse login(LoginRequest request) {
        var business = businessRepository.findByCode(request.businessCode().trim().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));
        if (!business.isActive())
            throw new BusinessSuspendedException("This business is suspended");
        var user = tenantExecutor.inTenant(business.getSchemaName(), () -> userRepository.findByUsername(request.username().trim())
                        .filter(u -> u.isActive() && passwordEncoder.matches(request.password(), u.getPasswordHash()))
                        .map(userMapper::toResponse))
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));
        var token = jwtService.issueBusinessToken(user.id(), business.getId(), user.role(), user.name());
        return new LoginResponse(token.token(), token.expiresAt(), user,
                new LoginResponse.BusinessInfo(business.getCode(), business.getName()));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformLoginResponse platformLogin(PlatformLoginRequest request) {
        var admin = platformAdminRepository.findByUsername(request.username().trim())
                .filter(a -> passwordEncoder.matches(request.password(), a.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));
        var token = jwtService.issuePlatformToken(admin.getId(), admin.getName());
        return new PlatformLoginResponse(token.token(), token.expiresAt(), admin.getName());
    }
}
