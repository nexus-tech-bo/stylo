package com.example.styloflow.config;

import com.example.styloflow.config.tenant.SchemaProvisioningService;
import com.example.styloflow.config.tenant.TenantExecutor;
import com.example.styloflow.entity.Business;
import com.example.styloflow.entity.PlatformAdmin;
import com.example.styloflow.entity.User;
import com.example.styloflow.enums.Role;
import com.example.styloflow.repository.BusinessRepository;
import com.example.styloflow.repository.PlatformAdminRepository;
import com.example.styloflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Idempotent development data: platform admin {@code superadmin} and business {@code demo} with user {@code admin}.
 * Opt-in only ({@code app.seed-dev-data=true}, e.g. {@code SEED_DEV_DATA=true} in the local {@code .env}).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed-dev-data", havingValue = "true")
@Order(1)
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {
    private final PlatformAdminRepository platformAdminRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final SchemaProvisioningService schemaProvisioningService;
    private final TenantExecutor tenantExecutor;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (!platformAdminRepository.existsByUsername("superadmin")) {
            platformAdminRepository.save(PlatformAdmin.builder()
                    .username("superadmin")
                    .passwordHash(passwordEncoder.encode("superadmin123"))
                    .name("Super Admin")
                    .build());
            log.info("Seeded platform admin 'superadmin'");
        }

        var business = businessRepository.findByCode("demo").orElseGet(() -> {
            var saved = businessRepository.save(Business.builder()
                    .code("demo")
                    .name("Peluquería Demo")
                    .currency("BOB")
                    .currencySymbol("Bs")
                    .taxRate(new BigDecimal("13.00"))
                    .build());
            saved.setSchemaName(SchemaProvisioningService.schemaName(saved.getId()));
            return businessRepository.save(saved);
        });
        schemaProvisioningService.provision(business.getId());

        tenantExecutor.inTenant(business.getSchemaName(), () -> {
            if (!userRepository.existsByUsername("admin")) {
                userRepository.save(User.builder()
                        .name("Admin")
                        .username("admin")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .role(Role.ADMIN)
                        .build());
                log.info("Seeded user 'admin' in {}", business.getSchemaName());
            }
            return null;
        });
    }
}
