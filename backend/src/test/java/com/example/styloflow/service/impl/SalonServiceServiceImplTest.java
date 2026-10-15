package com.example.styloflow.service.impl;

import com.example.styloflow.config.tenant.TenantContext;
import com.example.styloflow.dto.catalog.SalonServiceRequest;
import com.example.styloflow.entity.Category;
import com.example.styloflow.entity.SalonService;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.mapper.SalonServiceMapper;
import com.example.styloflow.repository.CategoryRepository;
import com.example.styloflow.repository.SalonServiceRepository;
import com.example.styloflow.service.ImageStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalonServiceServiceImplTest {
    private static final SalonServiceRequest REQUEST =
            new SalonServiceRequest(1L, "Corte dama", null, 45, new BigDecimal("80.00"), null);

    @Mock
    SalonServiceRepository salonServiceRepository;
    @Mock
    CategoryRepository categoryRepository;
    @Mock
    ImageStorageService imageStorageService;
    @Spy
    SalonServiceMapper salonServiceMapper = Mappers.getMapper(SalonServiceMapper.class);
    @InjectMocks
    SalonServiceServiceImpl service;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void createService_should_throwNotFound_and_notUpload_when_categoryMissing() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());
        var image = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> service.createService(REQUEST, image)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(imageStorageService);
        verify(salonServiceRepository, never()).save(any());
    }

    @Test
    void createService_should_uploadToTenantFolder_and_mapCategory() {
        TenantContext.setCurrentTenant("tenant_1");
        var image = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[]{1});
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(Category.builder().id(1L).name("Cortes").build()));
        when(imageStorageService.upload(image, "styloflow/tenant_1/services")).thenReturn("https://img/x.jpg");
        when(salonServiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.createService(REQUEST, image);

        assertThat(response.imageUrl()).isEqualTo("https://img/x.jpg");
        assertThat(response.category()).isEqualTo("Cortes");
        assertThat(response.categoryId()).isEqualTo(1L);
        assertThat(response.active()).isTrue();
    }

    @Test
    void updateServiceById_should_throwNotFound_when_serviceMissing() {
        when(salonServiceRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateServiceById(7L, REQUEST)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateServiceById_should_throwNotFound_when_categoryMissing() {
        when(salonServiceRepository.findById(7L)).thenReturn(Optional.of(SalonService.builder().id(7L).build()));
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateServiceById(7L, REQUEST)).isInstanceOf(NotFoundException.class);
        verify(salonServiceRepository, never()).save(any());
    }

    @Test
    void updateServiceById_should_keepImage() {
        var existing = SalonService.builder().id(7L).imageUrl("https://img/old.jpg").build();
        when(salonServiceRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(Category.builder().id(1L).name("Cortes").build()));
        when(salonServiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.updateServiceById(7L, REQUEST);

        assertThat(response.imageUrl()).isEqualTo("https://img/old.jpg");
        assertThat(response.name()).isEqualTo("Corte dama");
    }
}
