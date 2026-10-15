package com.example.styloflow.service;

import com.example.styloflow.dto.cashregister.CashRegisterResponse;
import com.example.styloflow.dto.cashregister.CloseCashRegisterRequest;
import com.example.styloflow.dto.cashregister.OpenCashRegisterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CashRegisterService {
    /** The open cash register with live totals, or empty when none is open. */
    Optional<CashRegisterResponse> findCurrentCashRegister();

    /** Opens a register; 422 if one is already open. */
    CashRegisterResponse openCashRegister(OpenCashRegisterRequest request, Long userId);

    /** Closes the open register, snapshotting the expected cash; 422 if none is open. */
    CashRegisterResponse closeCashRegister(CloseCashRegisterRequest request, Long userId);

    Page<CashRegisterResponse> findCashRegisterHistory(Pageable pageable);

    CashRegisterResponse findCashRegisterById(Long cashRegisterId);
}
