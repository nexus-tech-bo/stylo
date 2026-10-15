package com.example.styloflow.service;

import com.example.styloflow.dto.customer.CustomerRequest;
import com.example.styloflow.dto.customer.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {
    /** Lists customers, optionally filtered by {@code q} (case-insensitive contains on name, phone or tax id). */
    Page<CustomerResponse> findAllCustomers(String q, Pageable pageable);

    CustomerResponse findCustomerById(Long customerId);

    CustomerResponse createCustomer(CustomerRequest request);

    CustomerResponse updateCustomerById(Long customerId, CustomerRequest request);
}
