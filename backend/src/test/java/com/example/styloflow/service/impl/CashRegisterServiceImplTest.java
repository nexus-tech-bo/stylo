package com.example.styloflow.service.impl;

import com.example.styloflow.dto.cashregister.CashRegisterPaymentTotal;
import com.example.styloflow.dto.cashregister.CloseCashRegisterRequest;
import com.example.styloflow.dto.cashregister.OpenCashRegisterRequest;
import com.example.styloflow.entity.CashRegister;
import com.example.styloflow.entity.User;
import com.example.styloflow.enums.CashRegisterStatus;
import com.example.styloflow.enums.PaymentMethod;
import com.example.styloflow.exception.BusinessRuleException;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.repository.CashRegisterRepository;
import com.example.styloflow.repository.SaleRepository;
import com.example.styloflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashRegisterServiceImplTest {
    @Mock CashRegisterRepository cashRegisterRepository;
    @Mock SaleRepository saleRepository;
    @Mock UserRepository userRepository;
    @InjectMocks CashRegisterServiceImpl cashRegisterService;

    private final User admin = User.builder().id(1L).name("Admin").build();

    private CashRegister openRegister() {
        return CashRegister.builder().id(15L).openedBy(admin).openedAt(Instant.parse("2026-10-11T12:00:00Z"))
                .openingAmount(new BigDecimal("200.00")).build();
    }

    private static List<CashRegisterPaymentTotal> totals(Long registerId) {
        return List.of(
                new CashRegisterPaymentTotal(registerId, PaymentMethod.QR, 6L, new BigDecimal("580.00")),
                new CashRegisterPaymentTotal(registerId, PaymentMethod.CASH, 12L, new BigDecimal("1250.00")));
    }

    @Test
    void open_should_throw422_when_alreadyOpen() {
        when(cashRegisterRepository.existsByStatus(CashRegisterStatus.OPEN)).thenReturn(true);

        assertThatThrownBy(() -> cashRegisterService.openCashRegister(new OpenCashRegisterRequest(BigDecimal.TEN, null), 1L))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("already open");
        verify(cashRegisterRepository, never()).save(any());
    }

    @Test
    void open_should_createOpenRegister_when_noneOpen() {
        when(cashRegisterRepository.existsByStatus(CashRegisterStatus.OPEN)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(cashRegisterRepository.save(any())).thenAnswer(inv -> {
            CashRegister saved = inv.getArgument(0);
            saved.setId(15L);
            return saved;
        });

        var response = cashRegisterService.openCashRegister(new OpenCashRegisterRequest(new BigDecimal("200"), "Fondo"), 1L);

        assertThat(response.status()).isEqualTo(CashRegisterStatus.OPEN);
        assertThat(response.openedBy()).isEqualTo("Admin");
        assertThat(response.openingAmount()).isEqualTo(new BigDecimal("200.00"));
        assertThat(response.expectedCash()).isEqualByComparingTo("200.00");
        assertThat(response.salesCount()).isZero();
        assertThat(response.salesTotal()).isEqualByComparingTo("0");
        assertThat(response.difference()).isNull();
    }

    @Test
    void close_should_throw422_when_noneOpen() {
        when(cashRegisterRepository.findWithLockByStatus(CashRegisterStatus.OPEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cashRegisterService.closeCashRegister(new CloseCashRegisterRequest(BigDecimal.ONE, null), 1L))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("no open cash register");
    }

    @Test
    void close_should_snapshotExpectedCashAndDifference() {
        var register = openRegister();
        when(cashRegisterRepository.findWithLockByStatus(CashRegisterStatus.OPEN)).thenReturn(Optional.of(register));
        when(saleRepository.sumCompletedByPaymentMethod(List.of(15L))).thenReturn(totals(15L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(cashRegisterRepository.save(register)).thenReturn(register);

        var response = cashRegisterService.closeCashRegister(new CloseCashRegisterRequest(new BigDecimal("1450.50"), "Sin novedades"), 1L);

        assertThat(register.getStatus()).isEqualTo(CashRegisterStatus.CLOSED);
        assertThat(register.getExpectedCash()).isEqualByComparingTo("1450.00");
        assertThat(response.expectedCash()).isEqualByComparingTo("1450.00");
        assertThat(response.countedCash()).isEqualByComparingTo("1450.50");
        assertThat(response.difference()).isEqualByComparingTo("0.50");
        assertThat(response.salesCount()).isEqualTo(18);
        assertThat(response.salesTotal()).isEqualByComparingTo("1830.00");
        assertThat(response.closedBy()).isEqualTo("Admin");
        assertThat(response.closedAt()).isNotNull();
        assertThat(response.notes()).isEqualTo("Sin novedades");
        assertThat(response.byPaymentMethod()).extracting(t -> t.method()).containsExactly(PaymentMethod.CASH, PaymentMethod.QR);
    }

    @Test
    void current_should_computeLiveTotals_when_open() {
        when(cashRegisterRepository.findByStatus(CashRegisterStatus.OPEN)).thenReturn(Optional.of(openRegister()));
        when(saleRepository.sumCompletedByPaymentMethod(List.of(15L))).thenReturn(totals(15L));

        var response = cashRegisterService.findCurrentCashRegister().orElseThrow();

        assertThat(response.expectedCash()).isEqualByComparingTo("1450.00");
        assertThat(response.countedCash()).isNull();
        assertThat(response.difference()).isNull();
    }

    @Test
    void current_should_beEmpty_when_noneOpen() {
        when(cashRegisterRepository.findByStatus(CashRegisterStatus.OPEN)).thenReturn(Optional.empty());

        assertThat(cashRegisterService.findCurrentCashRegister()).isEmpty();
    }

    @Test
    void history_should_loadTotalsOfThePageInOneQuery() {
        var other = CashRegister.builder().id(14L).openedBy(admin).openingAmount(new BigDecimal("100.00"))
                .status(CashRegisterStatus.CLOSED).expectedCash(new BigDecimal("100.00")).countedCash(new BigDecimal("90.00")).build();
        var pageable = PageRequest.of(0, 20);
        when(cashRegisterRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(openRegister(), other), pageable, 2));
        when(saleRepository.sumCompletedByPaymentMethod(List.of(15L, 14L))).thenReturn(totals(15L));

        var page = cashRegisterService.findCashRegisterHistory(pageable);

        assertThat(page.getContent().getFirst().salesCount()).isEqualTo(18);
        assertThat(page.getContent().getLast().salesCount()).isZero();
        assertThat(page.getContent().getLast().difference()).isEqualByComparingTo("-10.00");
    }

    @Test
    void findById_should_throw404_when_missing() {
        when(cashRegisterRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cashRegisterService.findCashRegisterById(9L)).isInstanceOf(NotFoundException.class);
    }
}
