package com.example.styloflow.service.impl;

import com.example.styloflow.config.tenant.TenantContext;
import com.example.styloflow.dto.catalog.SalonServiceRequest;
import com.example.styloflow.dto.catalog.SalonServiceResponse;
import com.example.styloflow.entity.Category;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.mapper.SalonServiceMapper;
import com.example.styloflow.repository.CategoryRepository;
import com.example.styloflow.repository.SalonServiceRepository;
import com.example.styloflow.service.ImageStorageService;
import com.example.styloflow.service.SalonServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalonServiceServiceImpl implements SalonServiceService {
    private final SalonServiceRepository salonServiceRepository;
    private final CategoryRepository categoryRepository;
    private final ImageStorageService imageStorageService;
    private final SalonServiceMapper salonServiceMapper;

    @Override
    public List<SalonServiceResponse> findAllServices(boolean activeOnly) {
        var services = activeOnly
                ? salonServiceRepository.findByActiveTrueAndCategoryActiveTrueOrderByNameAsc()
                : salonServiceRepository.findAllByOrderByNameAsc();
        return salonServiceMapper.toResponseList(services);
    }

    @Override
    @Transactional
    public SalonServiceResponse createService(SalonServiceRequest request, MultipartFile image) {
        var service = salonServiceMapper.toEntity(request);
        service.setCategory(getCategoryOrThrowById(request.categoryId()));
        // Upload after the checks so a rejected request leaves no orphan image.
        service.setImageUrl(imageStorageService.upload(image, "styloflow/" + TenantContext.getCurrentTenant() + "/services"));
        return salonServiceMapper.toResponse(salonServiceRepository.save(service));
    }

    @Override
    @Transactional
    public SalonServiceResponse updateServiceById(Long serviceId, SalonServiceRequest request) {
        var service = salonServiceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found with id: " + serviceId));
        var category = getCategoryOrThrowById(request.categoryId());
        salonServiceMapper.toEntityUpdated(request, service);
        service.setCategory(category);
        return salonServiceMapper.toResponse(salonServiceRepository.save(service));
    }

    private Category getCategoryOrThrowById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + categoryId));
    }
}
