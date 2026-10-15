package com.example.styloflow.service.impl;

import com.example.styloflow.dto.customer.CustomerRequest;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.mapper.CustomerMapper;
import com.example.styloflow.mapper.CustomerMapperImpl;
import com.example.styloflow.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {
    @Mock CustomerRepository customerRepository;
    @Spy CustomerMapper customerMapper = new CustomerMapperImpl();
    @InjectMocks CustomerServiceImpl customerService;

    @Test
    void findAll_should_searchLowerCaseContains_when_qGiven() {
        when(customerRepository.search("%maría%", Pageable.unpaged())).thenReturn(Page.empty());

        customerService.findAllCustomers("  MaríA ", Pageable.unpaged());

        verify(customerRepository).search("%maría%", Pageable.unpaged());
    }

    @Test
    void findAll_should_notFilter_when_qBlank() {
        when(customerRepository.search(isNull(), eq(Pageable.unpaged()))).thenReturn(Page.empty());

        customerService.findAllCustomers(" ", Pageable.unpaged());

        verify(customerRepository).search(null, Pageable.unpaged());
    }

    @Test
    void update_should_throw404_when_missing() {
        when(customerRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.updateCustomerById(5L, new CustomerRequest("A", null, null, null, null)))
                .isInstanceOf(NotFoundException.class);
    }
}
