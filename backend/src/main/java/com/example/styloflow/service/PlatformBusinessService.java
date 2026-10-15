package com.example.styloflow.service;

import com.example.styloflow.dto.platform.BusinessSummaryResponse;
import com.example.styloflow.dto.platform.CreateBusinessRequest;
import com.example.styloflow.dto.platform.StatusRequest;

import java.util.List;

/** Platform (SUPERADMIN) management of businesses. */
public interface PlatformBusinessService {
    List<BusinessSummaryResponse> findAllBusinesses();

    /**
     * Creates the business, provisions its schema and creates its ADMIN user (optionally a base catalog).
     *
     * @throws com.example.styloflow.exception.ConflictException duplicate code
     */
    BusinessSummaryResponse createBusiness(CreateBusinessRequest request);

    void updateBusinessStatus(Long businessId, StatusRequest request);
}
