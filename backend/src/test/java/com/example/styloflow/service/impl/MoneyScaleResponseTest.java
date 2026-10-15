package com.example.styloflow.service.impl;

import com.example.styloflow.dto.business.BusinessRequest;
import com.example.styloflow.dto.catalog.ProductRequest;
import com.example.styloflow.dto.catalog.SalonServiceRequest;
import com.example.styloflow.dto.user.UserRequest;
import com.example.styloflow.entity.Business;
import com.example.styloflow.entity.Category;
import com.example.styloflow.enums.Role;
import com.example.styloflow.mapper.BusinessMapper;
import com.example.styloflow.mapper.ProductMapper;
import com.example.styloflow.mapper.SalonServiceMapper;
import com.example.styloflow.mapper.UserMapper;
import com.example.styloflow.repository.BusinessRepository;
import com.example.styloflow.repository.CategoryRepository;
import com.example.styloflow.repository.ProductRepository;
import com.example.styloflow.repository.SalonServiceRepository;
import com.example.styloflow.repository.UserRepository;
import com.example.styloflow.service.ImageStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Bug found in the live smoke: API.md says money (and percentages) always have 2 decimals, but create/update
 * responses echo the scale the client sent, because the saved entity is mapped without reloading it:
 * {@code price=10} answers {@code "price":10}, {@code commissionRate:40} answers {@code 40}, {@code taxRate:13}
 * answers {@code 13}. Reads from the database (GET) do come back as {@code 10.00}.
 */
@DisplayName("Money scale in create/update responses")
class MoneyScaleResponseTest {

    @Test
    @DisplayName("POST /api/catalog/products with price=10 answers price 10.00")
    void createProduct_should_returnPriceWithTwoDecimals() {
        var repo = mock(ProductRepository.class);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var service = new ProductServiceImpl(repo, mock(ImageStorageService.class), Mappers.getMapper(ProductMapper.class));

        var response = service.createProduct(new ProductRequest("Gel", null, new BigDecimal("10"), 5, null, null), null);

        assertThat(response.price().scale()).as("price %s", response.price().toPlainString()).isEqualTo(2);
    }

    @Test
    @DisplayName("POST /api/catalog/services with price=10.5 answers price 10.50")
    void createService_should_returnPriceWithTwoDecimals() {
        var repo = mock(SalonServiceRepository.class);
        var categories = mock(CategoryRepository.class);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(categories.findById(1L)).thenReturn(Optional.of(Category.builder().id(1L).name("Cortes").build()));
        var service = new SalonServiceServiceImpl(repo, categories, mock(ImageStorageService.class), Mappers.getMapper(SalonServiceMapper.class));

        var response = service.createService(new SalonServiceRequest(1L, "Corte", null, 30, new BigDecimal("10.5"), true), null);

        assertThat(response.price().scale()).as("price %s", response.price().toPlainString()).isEqualTo(2);
    }

    @Test
    @DisplayName("POST /api/users with commissionRate=40 answers 40.00 (and 0.00 when omitted)")
    void createUser_should_returnCommissionRateWithTwoDecimals() {
        var repo = mock(UserRepository.class);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var service = new UserServiceImpl(repo, Mappers.getMapper(UserMapper.class), mock(PasswordEncoder.class));

        var stylist = service.createUser(new UserRequest("Ana", "ana", "secreto1", Role.STYLIST, null, new BigDecimal("40"), null));
        var cashier = service.createUser(new UserRequest("Eva", "eva", "secreto1", Role.CASHIER, null, null, null));

        assertThat(stylist.commissionRate().scale()).as("commissionRate %s", stylist.commissionRate()).isEqualTo(2);
        assertThat(cashier.commissionRate().scale()).as("default commissionRate %s", cashier.commissionRate()).isEqualTo(2);
    }

    @Test
    @DisplayName("PUT /api/business with taxRate=13 answers 13.00")
    void updateBusiness_should_returnTaxRateWithTwoDecimals() {
        var repo = mock(BusinessRepository.class);
        when(repo.findById(1L)).thenReturn(Optional.of(Business.builder().id(1L).code("demo").name("Demo").build()));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var service = new BusinessServiceImpl(repo, Mappers.getMapper(BusinessMapper.class));

        var response = service.updateBusinessById(1L,
                new BusinessRequest("Demo", null, null, null, "BOB", "Bs", new BigDecimal("13"), null));

        assertThat(response.taxRate().scale()).as("taxRate %s", response.taxRate()).isEqualTo(2);
    }
}
