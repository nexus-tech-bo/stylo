package com.example.styloflow.service.impl;

import com.example.styloflow.dto.cashregister.CashRegisterPaymentTotal;
import com.example.styloflow.dto.cashregister.CashRegisterResponse;
import com.example.styloflow.dto.cashregister.CloseCashRegisterRequest;
import com.example.styloflow.dto.cashregister.OpenCashRegisterRequest;
import com.example.styloflow.dto.cashregister.PaymentMethodTotalResponse;
import com.example.styloflow.entity.CashRegister;
import com.example.styloflow.entity.User;
import com.example.styloflow.enums.CashRegisterStatus;
import com.example.styloflow.enums.PaymentMethod;
import com.example.styloflow.exception.BusinessRuleException;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.repository.CashRegisterRepository;
import com.example.styloflow.repository.SaleRepository;
import com.example.styloflow.repository.UserRepository;
import com.example.styloflow.service.CashRegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashRegisterServiceImpl implements CashRegisterService {
    private final CashRegisterRepository cashRegisterRepository;
    private final SaleRepository saleRepository;
    private final UserRepository userRepository;

    @Override
    public Optional<CashRegisterResponse> findCurrentCashRegister() {
        return cashRegisterRepository.findByStatus(CashRegisterStatus.OPEN).map(this::toResponse);
    }

    @Override
    @Transactional
    public CashRegisterResponse openCashRegister(OpenCashRegisterRequest request, Long userId) {
        // The partial unique index is the real guard: a concurrent open ends as DataIntegrityViolation -> 409.
        if (cashRegisterRepository.existsByStatus(CashRegisterStatus.OPEN))
            throw new BusinessRuleException("A cash register is already open");
        var register = CashRegister.builder()
                .openedBy(getUserOrThrowById(userId))
                .openedAt(now())
                .openingAmount(money(request.openingAmount()))
                .notes(request.notes())
                .build();
        return toResponse(cashRegisterRepository.save(register));
    }

    @Override
    @Transactional
    public CashRegisterResponse closeCashRegister(CloseCashRegisterRequest request, Long userId) {
        var register = cashRegisterRepository.findWithLockByStatus(CashRegisterStatus.OPEN)
                .orElseThrow(() -> new BusinessRuleException("There is no open cash register"));
        var totals = saleRepository.sumCompletedByPaymentMethod(List.of(register.getId()));
        register.setExpectedCash(register.getOpeningAmount().add(cashTotal(totals)));
        register.setCountedCash(money(request.countedCash()));
        register.setClosedBy(getUserOrThrowById(userId));
        register.setClosedAt(now());
        register.setStatus(CashRegisterStatus.CLOSED);
        if (request.notes() != null)
            register.setNotes(request.notes());
        return toResponse(cashRegisterRepository.save(register), totals);
    }

    @Override
    public Page<CashRegisterResponse> findCashRegisterHistory(Pageable pageable) {
        var page = cashRegisterRepository.findAll(pageable);
        var ids = page.map(CashRegister::getId).toList();
        // One aggregate query for the whole page instead of one per register.
        var totalsByRegister = ids.isEmpty()
                ? Map.<Long, List<CashRegisterPaymentTotal>>of()
                : saleRepository.sumCompletedByPaymentMethod(ids).stream()
                        .collect(Collectors.groupingBy(CashRegisterPaymentTotal::cashRegisterId));
        return page.map(r -> toResponse(r, totalsByRegister.getOrDefault(r.getId(), List.of())));
    }

    @Override
    public CashRegisterResponse findCashRegisterById(Long cashRegisterId) {
        return cashRegisterRepository.findById(cashRegisterId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Cash register not found with id: " + cashRegisterId));
    }

    private CashRegisterResponse toResponse(CashRegister register) {
        return toResponse(register, saleRepository.sumCompletedByPaymentMethod(List.of(register.getId())));
    }

    private CashRegisterResponse toResponse(CashRegister register, List<CashRegisterPaymentTotal> totals) {
        var byMethod = totals.stream()
                .sorted(Comparator.comparing(CashRegisterPaymentTotal::method))
                .map(t -> new PaymentMethodTotalResponse(t.method(), t.count(), money(t.total())))
                .toList();
        var salesCount = byMethod.stream().mapToLong(PaymentMethodTotalResponse::count).sum();
        var salesTotal = byMethod.stream().map(PaymentMethodTotalResponse::total).reduce(money(BigDecimal.ZERO), BigDecimal::add);
        var expectedCash = register.getExpectedCash() != null
                ? register.getExpectedCash()
                : register.getOpeningAmount().add(cashTotal(totals));
        var difference = register.getCountedCash() != null ? register.getCountedCash().subtract(expectedCash) : null;
        return new CashRegisterResponse(register.getId(), register.getStatus(),
                name(register.getOpenedBy()), register.getOpenedAt(), register.getOpeningAmount(),
                name(register.getClosedBy()), register.getClosedAt(),
                salesCount, salesTotal, expectedCash, register.getCountedCash(), difference,
                byMethod, register.getNotes());
    }

    private static BigDecimal cashTotal(List<CashRegisterPaymentTotal> totals) {
        return totals.stream()
                .filter(t -> t.method() == PaymentMethod.CASH)
                .map(CashRegisterPaymentTotal::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private User getUserOrThrowById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
    }

    private static String name(User user) {
        return user != null ? user.getName() : null;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
