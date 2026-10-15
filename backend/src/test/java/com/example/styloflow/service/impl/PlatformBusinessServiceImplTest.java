package com.example.styloflow.service.impl;

import com.example.styloflow.config.tenant.SchemaProvisioningService;
import com.example.styloflow.config.tenant.TenantExecutor;
import com.example.styloflow.dto.platform.CreateBusinessRequest;
import com.example.styloflow.dto.platform.StatusRequest;
import com.example.styloflow.entity.Business;
import com.example.styloflow.entity.User;
import com.example.styloflow.enums.Role;
import com.example.styloflow.exception.ConflictException;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlatformBusinessServiceImplTest {
    @Mock
    BusinessRepository businessRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    PlatformStatsRepository platformStatsRepository;
    @Mock
    CategoryRepository categoryRepository;
    @Mock
    SalonServiceRepository salonServiceRepository;
    @Mock
    ProductRepository productRepository;
    @Mock
    SchemaProvisioningService schemaProvisioningService;
    @Mock
    TenantExecutor tenantExecutor;
    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    PlatformBusinessServiceImpl service;

    @BeforeEach
    void setUp() {
        when(tenantExecutor.inTenant(anyString(), any())).thenAnswer(inv -> inv.<Supplier<?>>getArgument(1).get());
        when(businessRepository.save(any())).thenAnswer(inv -> {
            Business b = inv.getArgument(0);
            if (b.getId() == null) b.setId(7L);
            return b;
        });
    }

    private static CreateBusinessRequest request(Boolean baseCatalog) {
        return new CreateBusinessRequest("Salón Bella", "salon-bella", null, null, "Laura", "laura", "clave123", baseCatalog);
    }

    @Test
    void createBusiness_should_throw409_when_codeTaken() {
        when(businessRepository.existsByCode("salon-bella")).thenReturn(true);

        assertThatThrownBy(() -> service.createBusiness(request(null))).isInstanceOf(ConflictException.class);
        verify(businessRepository, never()).save(any());
    }

    @Test
    void createBusiness_should_saveProvisionThenCreateAdmin_when_valid() {
        when(passwordEncoder.encode("clave123")).thenReturn("encoded");

        var response = service.createBusiness(request(true));

        InOrder order = inOrder(businessRepository, schemaProvisioningService, userRepository);
        order.verify(businessRepository, times(2)).save(any());
        order.verify(schemaProvisioningService).provision(7L);
        var admin = ArgumentCaptor.forClass(User.class);
        order.verify(userRepository).save(admin.capture());
        assertThat(admin.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getValue().getPasswordHash()).isEqualTo("encoded");
        verify(tenantExecutor).inTenant(eq("tenant_7"), any());
        verify(categoryRepository).saveAll(anyList());
        assertThat(response.users()).isEqualTo(1);
        assertThat(response.total30d()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void createBusiness_should_notSeedCatalog_when_baseCatalogOmitted() {
        service.createBusiness(request(null));

        verifyNoInteractions(categoryRepository, salonServiceRepository, productRepository);
    }

    @Test
    void createBusiness_should_deleteBusiness_when_provisioningFails() {
        when(schemaProvisioningService.provision(7L)).thenThrow(new IllegalStateException("flyway down"));

        assertThatThrownBy(() -> service.createBusiness(request(null))).isInstanceOf(IllegalStateException.class);
        verify(businessRepository).deleteById(7L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateBusinessStatus_should_throw404_when_missing() {
        when(businessRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateBusinessStatus(9L, new StatusRequest(false)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateBusinessStatus_should_suspend_when_activeFalse() {
        var business = Business.builder().id(2L).code("x").name("X").build();
        when(businessRepository.findById(2L)).thenReturn(Optional.of(business));

        service.updateBusinessStatus(2L, new StatusRequest(false));

        assertThat(business.isActive()).isFalse();
    }
}
