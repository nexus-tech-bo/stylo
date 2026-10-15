package com.example.styloflow.service;

import com.example.styloflow.dto.business.BusinessRequest;
import com.example.styloflow.dto.business.BusinessResponse;

/** Settings of the business of the token (row in {@code public.businesses}). */
public interface BusinessService {
    BusinessResponse findBusinessById(Long businessId);

    BusinessResponse updateBusinessById(Long businessId, BusinessRequest request);
}
