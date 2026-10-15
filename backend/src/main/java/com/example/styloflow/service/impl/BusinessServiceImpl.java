package com.example.styloflow.service.impl;

import com.example.styloflow.dto.business.BusinessRequest;
import com.example.styloflow.dto.business.BusinessResponse;
import com.example.styloflow.entity.Business;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.mapper.BusinessMapper;
import com.example.styloflow.repository.BusinessRepository;
import com.example.styloflow.service.BusinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessServiceImpl implements BusinessService {
    private final BusinessRepository businessRepository;
    private final BusinessMapper businessMapper;

    @Override
    public BusinessResponse findBusinessById(Long businessId) {
        return businessMapper.toResponse(getBusinessOrThrowById(businessId));
    }

    @Override
    @Transactional
    public BusinessResponse updateBusinessById(Long businessId, BusinessRequest request) {
        var business = getBusinessOrThrowById(businessId);
        businessMapper.toEntityUpdated(request, business);
        return businessMapper.toResponse(businessRepository.save(business));
    }

    private Business getBusinessOrThrowById(Long businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new NotFoundException("Business not found with id: " + businessId));
    }
}
