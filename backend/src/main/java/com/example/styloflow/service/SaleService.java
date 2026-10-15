package com.example.styloflow.service;

import com.example.styloflow.dto.sale.SaleRequest;
import com.example.styloflow.dto.sale.SaleResponse;
import com.example.styloflow.dto.sale.SaleSummaryResponse;
import com.example.styloflow.dto.sale.VoidSaleRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface SaleService {
    /**
     * Registers a sale in the open cash register and decreases product stock.
     *
     * @param cashierId  authenticated user
     * @param businessId business of the token (for its tax rate)
     * @throws com.example.styloflow.exception.BusinessRuleException on any rule of the API contract (422)
     */
    SaleResponse createSale(SaleRequest request, Long cashierId, Long businessId);

    /** Sales between {@code from} and {@code to} (business days, default today), optionally of one customer. */
    Page<SaleSummaryResponse> findAllSales(LocalDate from, LocalDate to, Long customerId, Pageable pageable);

    SaleResponse findSaleById(Long saleId);

    /** Voids a completed sale whose register is still open and restores product stock. */
    SaleResponse voidSaleById(Long saleId, VoidSaleRequest request, Long userId);
}
