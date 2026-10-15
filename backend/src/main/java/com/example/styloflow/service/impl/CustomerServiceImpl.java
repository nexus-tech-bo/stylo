package com.example.styloflow.service.impl;

import com.example.styloflow.dto.customer.CustomerRequest;
import com.example.styloflow.dto.customer.CustomerResponse;
import com.example.styloflow.entity.Customer;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.mapper.CustomerMapper;
import com.example.styloflow.repository.CustomerRepository;
import com.example.styloflow.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    public Page<CustomerResponse> findAllCustomers(String q, Pageable pageable) {
        // ponytail: % and _ typed by the user act as LIKE wildcards; escape them if that ever matters
        var pattern = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        return customerRepository.search(pattern, pageable).map(customerMapper::toResponse);
    }

    @Override
    public CustomerResponse findCustomerById(Long customerId) {
        return customerMapper.toResponse(getCustomerOrThrowById(customerId));
    }

    @Override
    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        return customerMapper.toResponse(customerRepository.save(customerMapper.toEntity(request)));
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomerById(Long customerId, CustomerRequest request) {
        var customer = getCustomerOrThrowById(customerId);
        customerMapper.toEntityUpdated(request, customer);
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    private Customer getCustomerOrThrowById(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + customerId));
    }
}
