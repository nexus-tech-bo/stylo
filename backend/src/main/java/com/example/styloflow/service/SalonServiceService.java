package com.example.styloflow.service;

import com.example.styloflow.dto.catalog.SalonServiceRequest;
import com.example.styloflow.dto.catalog.SalonServiceResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SalonServiceService {
    /**
     * Services sorted by name.
     *
     * @param activeOnly {@code true} keeps only active services of active categories
     */
    List<SalonServiceResponse> findAllServices(boolean activeOnly);

    /**
     * @param image optional image; {@code null} or empty means no image
     * @throws com.example.styloflow.exception.NotFoundException if the category does not exist
     */
    SalonServiceResponse createService(SalonServiceRequest request, MultipartFile image);

    /** Replaces every field except the image. */
    SalonServiceResponse updateServiceById(Long serviceId, SalonServiceRequest request);
}
