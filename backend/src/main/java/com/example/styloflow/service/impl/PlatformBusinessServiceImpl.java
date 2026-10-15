package com.example.styloflow.service.impl;

import com.example.styloflow.config.tenant.SchemaProvisioningService;
import com.example.styloflow.config.tenant.TenantExecutor;
import com.example.styloflow.dto.platform.BusinessSummaryResponse;
import com.example.styloflow.dto.platform.CreateBusinessRequest;
import com.example.styloflow.dto.platform.StatusRequest;
import com.example.styloflow.entity.*;
import com.example.styloflow.enums.Role;
import com.example.styloflow.enums.SaleStatus;
import com.example.styloflow.exception.ConflictException;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.repository.*;
import com.example.styloflow.service.PlatformBusinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * No class-level transaction: creating a business spans {@code public}, schema provisioning (which must run
 * outside any transaction) and the new tenant schema, and the listing reads every tenant schema.
 * Each step runs in its own transaction through {@link TenantExecutor}.
 */
@Service
@RequiredArgsConstructor
public class PlatformBusinessServiceImpl implements PlatformBusinessService {
    private static final String PUBLIC_SCHEMA = "public";

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final PlatformStatsRepository platformStatsRepository;
    private final CategoryRepository categoryRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final ProductRepository productRepository;
    private final SchemaProvisioningService schemaProvisioningService;
    private final TenantExecutor tenantExecutor;
    private final PasswordEncoder passwordEncoder;

    // ponytail: one small transaction per tenant; fine for tens of businesses, a cached/summary table if it reaches hundreds.
    @Override
    public List<BusinessSummaryResponse> findAllBusinesses() {
        var since = Instant.now().minus(30, ChronoUnit.DAYS);
        return businessRepository.findAll(Sort.by("id")).stream()
                .map(business -> tenantExecutor.inTenant(business.getSchemaName(), () -> {
                    var stats = platformStatsRepository.salesSince(SaleStatus.COMPLETED, since);
                    return toSummary(business, userRepository.count(), stats.getSalesCount(), stats.getSalesTotal());
                }))
                .toList();
    }

    @Override
    public BusinessSummaryResponse createBusiness(CreateBusinessRequest request) {
        if (businessRepository.existsByCode(request.code()))
            throw new ConflictException("Business code already exists: " + request.code());
        var business = tenantExecutor.inTenant(PUBLIC_SCHEMA, () -> {
            var saved = businessRepository.save(Business.builder()
                    .code(request.code())
                    .name(request.name())
                    .taxId(request.taxId())
                    .phone(request.phone())
                    .build());
            saved.setSchemaName(SchemaProvisioningService.schemaName(saved.getId()));
            return businessRepository.save(saved);
        });
        try {
            schemaProvisioningService.provision(business.getId());
            tenantExecutor.inTenant(business.getSchemaName(), () -> {
                userRepository.save(User.builder()
                        .name(request.adminName())
                        .username(request.adminUsername())
                        .passwordHash(passwordEncoder.encode(request.adminPassword()))
                        .role(Role.ADMIN)
                        .build());
                if (Boolean.TRUE.equals(request.baseCatalog())) seedBaseCatalog();
                return null;
            });
        } catch (RuntimeException e) {
            // Without its schema or admin the business is unusable and its code would stay taken.
            businessRepository.deleteById(business.getId());
            throw e;
        }
        return toSummary(business, 1, 0, BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public void updateBusinessStatus(Long businessId, StatusRequest request) {
        var business = businessRepository.findById(businessId)
                .orElseThrow(() -> new NotFoundException("Business not found with id: " + businessId));
        business.setActive(request.active());
        businessRepository.save(business);
    }

    private void seedBaseCatalog() {
        var cuts = Category.builder().name("Cortes").build();
        var color = Category.builder().name("Coloración").build();
        var care = Category.builder().name("Tratamientos").build();
        categoryRepository.saveAll(List.of(cuts, color, care));
        salonServiceRepository.saveAll(List.of(
                service(cuts, "Corte dama", 45, "80.00"),
                service(cuts, "Corte caballero", 30, "40.00"),
                service(color, "Tinte completo", 120, "250.00"),
                service(color, "Mechas", 150, "350.00"),
                service(care, "Hidratación capilar", 60, "120.00")));
        productRepository.saveAll(List.of(
                product("Shampoo profesional", "65.00"),
                product("Acondicionador", "60.00"),
                product("Cera para peinar", "45.00")));
    }

    private static SalonService service(Category category, String name, int minutes, String price) {
        return SalonService.builder().category(category).name(name).durationMinutes(minutes).price(new BigDecimal(price)).build();
    }

    private static Product product(String name, String price) {
        return Product.builder().name(name).price(new BigDecimal(price)).build();
    }

    private static BusinessSummaryResponse toSummary(Business business, long users, long sales, BigDecimal total) {
        return new BusinessSummaryResponse(business.getId(), business.getCode(), business.getName(), business.isActive(),
                business.getCreatedAt(), users, sales, total.setScale(2, RoundingMode.HALF_UP));
    }
}
