package com.example.styloflow.service.impl;

import com.example.styloflow.dto.sale.SaleItemRequest;
import com.example.styloflow.dto.sale.SaleRequest;
import com.example.styloflow.dto.sale.VoidSaleRequest;
import com.example.styloflow.entity.Business;
import com.example.styloflow.entity.CashRegister;
import com.example.styloflow.entity.Product;
import com.example.styloflow.entity.Sale;
import com.example.styloflow.entity.SaleItem;
import com.example.styloflow.entity.SalonService;
import com.example.styloflow.entity.User;
import com.example.styloflow.enums.CashRegisterStatus;
import com.example.styloflow.enums.ItemType;
import com.example.styloflow.enums.PaymentMethod;
import com.example.styloflow.enums.Role;
import com.example.styloflow.enums.SaleStatus;
import com.example.styloflow.exception.BusinessRuleException;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.mapper.SaleMapper;
import com.example.styloflow.mapper.SaleMapperImpl;
import com.example.styloflow.repository.BusinessRepository;
import com.example.styloflow.repository.CashRegisterRepository;
import com.example.styloflow.repository.CustomerRepository;
import com.example.styloflow.repository.ProductRepository;
import com.example.styloflow.repository.SaleRepository;
import com.example.styloflow.repository.SalonServiceRepository;
import com.example.styloflow.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleServiceImplTest {
    private static final Long CASHIER_ID = 1L;
    private static final Long BUSINESS_ID = 1L;

    @Mock SaleRepository saleRepository;
    @Mock CashRegisterRepository cashRegisterRepository;
    @Mock CustomerRepository customerRepository;
    @Mock UserRepository userRepository;
    @Mock SalonServiceRepository salonServiceRepository;
    @Mock ProductRepository productRepository;
    @Mock BusinessRepository businessRepository;
    @Spy SaleMapper saleMapper = new SaleMapperImpl();
    @InjectMocks SaleServiceImpl saleService;

    private final CashRegister register = CashRegister.builder().id(15L).status(CashRegisterStatus.OPEN).build();
    private final User cashier = User.builder().id(CASHIER_ID).name("Admin").role(Role.ADMIN).build();
    private final User stylist = User.builder().id(3L).name("Ana Pérez").role(Role.STYLIST)
            .commissionRate(new BigDecimal("40.00")).build();
    private final SalonService haircut = SalonService.builder().id(7L).name("Corte dama").price(new BigDecimal("80.00")).build();
    private final Product shampoo = Product.builder().id(4L).name("Shampoo").price(new BigDecimal("85.00")).stock(5).build();

    private static SaleItemRequest service(BigDecimal unitPrice, BigDecimal discount, Long stylistId) {
        return new SaleItemRequest(ItemType.SERVICE, 7L, 1, unitPrice, discount, stylistId);
    }

    private static SaleItemRequest product(int quantity) {
        return new SaleItemRequest(ItemType.PRODUCT, 4L, quantity, null, null, null);
    }

    private static SaleRequest sale(List<SaleItemRequest> items, String discount, PaymentMethod method, String received) {
        return new SaleRequest(null, items, discount == null ? null : new BigDecimal(discount), method,
                received == null ? null : new BigDecimal(received), null);
    }

    private void givenOpenRegisterAndCashier() {
        when(cashRegisterRepository.findWithLockByStatus(CashRegisterStatus.OPEN)).thenReturn(Optional.of(register));
        when(userRepository.findById(CASHIER_ID)).thenReturn(Optional.of(cashier));
    }

    private void givenTaxRate(String rate) {
        when(businessRepository.findById(BUSINESS_ID))
                .thenReturn(Optional.of(Business.builder().id(BUSINESS_ID).taxRate(new BigDecimal(rate)).build()));
        when(saleRepository.save(any(Sale.class))).then(returnsFirstArg());
    }

    @Nested
    class CreateSale {
        @Test
        void should_computeMoneyStockAndSnapshots_when_valid() {
            givenOpenRegisterAndCashier();
            givenTaxRate("13.00");
            when(salonServiceRepository.findById(7L)).thenReturn(Optional.of(haircut));
            when(productRepository.findById(4L)).thenReturn(Optional.of(shampoo));
            when(userRepository.findById(3L)).thenReturn(Optional.of(stylist));

            var response = saleService.createSale(sale(List.of(
                    service(new BigDecimal("90"), new BigDecimal("10"), 3L), product(2)), "5", PaymentMethod.CASH, "300"),
                    CASHIER_ID, BUSINESS_ID);

            // 90 - 10 = 80; 85 x 2 = 170; subtotal 250; total 245; tax 245 x 13 / 113 = 28.19; change 55
            assertThat(response.subtotal()).isEqualByComparingTo("250.00");
            assertThat(response.discount()).isEqualByComparingTo("5.00");
            assertThat(response.total()).isEqualByComparingTo("245.00");
            assertThat(response.tax()).isEqualTo(new BigDecimal("28.19"));
            assertThat(response.amountReceived()).isEqualByComparingTo("300.00");
            assertThat(response.change()).isEqualByComparingTo("55.00");
            assertThat(response.cashRegisterId()).isEqualTo(15L);
            assertThat(response.cashier()).isEqualTo("Admin");
            assertThat(response.items()).hasSize(2);
            var first = response.items().getFirst();
            assertThat(first.description()).isEqualTo("Corte dama");
            assertThat(first.unitPrice()).isEqualByComparingTo("90.00");
            assertThat(first.subtotal()).isEqualByComparingTo("80.00");
            assertThat(first.stylist()).isEqualTo("Ana Pérez");
            assertThat(response.items().getLast().unitPrice()).isEqualByComparingTo("85.00");
            assertThat(shampoo.getStock()).isEqualTo(3);
        }

        @Test
        void should_snapshotStylistCommissionRate_when_stylistSet() {
            givenOpenRegisterAndCashier();
            givenTaxRate("0");
            when(salonServiceRepository.findById(7L)).thenReturn(Optional.of(haircut));
            when(userRepository.findById(3L)).thenReturn(Optional.of(stylist));

            saleService.createSale(sale(List.of(service(null, null, 3L)), null, PaymentMethod.QR, null), CASHIER_ID, BUSINESS_ID);

            var saved = ArgumentCaptor.forClass(Sale.class);
            verify(saleRepository).save(saved.capture());
            SaleItem item = saved.getValue().getItems().getFirst();
            assertThat(item.getCommissionRate()).isEqualByComparingTo("40.00");
            assertThat(item.getSale()).isSameAs(saved.getValue());
        }

        @Test
        void should_defaultReceivedToTotalAndNoChangeOutsideCash() {
            givenOpenRegisterAndCashier();
            givenTaxRate("0");
            when(salonServiceRepository.findById(7L)).thenReturn(Optional.of(haircut));

            var cash = saleService.createSale(sale(List.of(service(null, null, null)), null, PaymentMethod.CASH, null), CASHIER_ID, BUSINESS_ID);
            var card = saleService.createSale(sale(List.of(service(null, null, null)), null, PaymentMethod.CARD, null), CASHIER_ID, BUSINESS_ID);

            assertThat(cash.amountReceived()).isEqualByComparingTo("80.00");
            assertThat(cash.change()).isEqualByComparingTo("0.00");
            assertThat(cash.tax()).isEqualByComparingTo("0.00");
            assertThat(card.change()).isNull();
        }

        @Test
        void should_throw422_when_noOpenRegister() {
            when(cashRegisterRepository.findWithLockByStatus(CashRegisterStatus.OPEN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> saleService.createSale(sale(List.of(product(1)), null, PaymentMethod.CASH, null), CASHIER_ID, BUSINESS_ID))
                    .isInstanceOf(BusinessRuleException.class).hasMessageContaining("no open cash register");
            verify(saleRepository, never()).save(any());
        }

        @Test
        void should_throw422_when_serviceInactive() {
            givenOpenRegisterAndCashier();
            haircut.setActive(false);
            when(salonServiceRepository.findById(7L)).thenReturn(Optional.of(haircut));

            assertThatThrownBy(() -> saleService.createSale(sale(List.of(service(null, null, null)), null, PaymentMethod.CASH, null), CASHIER_ID, BUSINESS_ID))
                    .isInstanceOf(BusinessRuleException.class).hasMessageContaining("inactive");
        }

        @Test
        void should_throw422_when_productInactive() {
            givenOpenRegisterAndCashier();
            shampoo.setActive(false);
            when(productRepository.findById(4L)).thenReturn(Optional.of(shampoo));

            assertThatThrownBy(() -> saleService.createSale(sale(List.of(product(1)), null, PaymentMethod.CASH, null), CASHIER_ID, BUSINESS_ID))
                    .isInstanceOf(BusinessRuleException.class).hasMessageContaining("inactive");
        }

        @Test
        void should_throw404_when_productMissing() {
            givenOpenRegisterAndCashier();
            when(productRepository.findById(4L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> saleService.createSale(sale(List.of(product(1)), null, PaymentMethod.CASH, null), CASHIER_ID, BUSINESS_ID))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void should_throw422_when_stylistIsNotAStylist() {
            givenOpenRegisterAndCashier();
            when(salonServiceRepository.findById(7L)).thenReturn(Optional.of(haircut));
            when(userRepository.findById(3L)).thenReturn(Optional.of(User.builder().id(3L).role(Role.CASHIER).build()));

            assertThatThrownBy(() -> saleService.createSale(sale(List.of(service(null, null, 3L)), null, PaymentMethod.CASH, null), CASHIER_ID, BUSINESS_ID))
                    .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Invalid stylist");
        }

        @Test
        void should_throw422_when_stylistInactive() {
            givenOpenRegisterAndCashier();
            stylist.setActive(false);
            when(salonServiceRepository.findById(7L)).thenReturn(Optional.of(haircut));
            when(userRepository.findById(3L)).thenReturn(Optional.of(stylist));

            assertThatThrownBy(() -> saleService.createSale(sale(List.of(service(null, null, 3L)), null, PaymentMethod.CASH, null), CASHIER_ID, BUSINESS_ID))
                    .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Invalid stylist");
        }

        @Test
        void should_throw422_when_stylistMissing() {
            givenOpenRegisterAndCashier();
            when(salonServiceRepository.findById(7L)).thenReturn(Optional.of(haircut));
            when(userRepository.findById(3L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> saleService.createSale(sale(List.of(service(null, null, 3L)), null, PaymentMethod.CASH, null), CASHIER_ID, BUSINESS_ID))
                    .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Invalid stylist");
        }

        @Test
        void should_throw422_when_lineDiscountExceedsLineAmount() {
            givenOpenRegisterAndCashier();
            when(salonServiceRepository.findById(7L)).thenReturn(Optional.of(haircut));

            assertThatThrownBy(() -> saleService.createSale(sale(List.of(service(null, new BigDecimal("80.01"), null)), null, PaymentMethod.CASH, null), CASHIER_ID, BUSINESS_ID))
                    .isInstanceOf(BusinessRuleException.class).hasMessageContaining("exceeds its amount");
        }

        @Test
        void should_throw422_when_globalDiscountExceedsSubtotal() {
            givenOpenRegisterAndCashier();
            when(salonServiceRepository.findById(7L)).thenReturn(Optional.of(haircut));

            assertThatThrownBy(() -> saleService.createSale(sale(List.of(service(null, null, null)), "80.01", PaymentMethod.CASH, null), CASHIER_ID, BUSINESS_ID))
                    .isInstanceOf(BusinessRuleException.class).hasMessageContaining("exceeds the subtotal");
        }

        @Test
        void should_throw422_when_cashReceivedLessThanTotal() {
            givenOpenRegisterAndCashier();
            when(salonServiceRepository.findById(7L)).thenReturn(Optional.of(haircut));
            when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.of(Business.builder().build()));

            assertThatThrownBy(() -> saleService.createSale(sale(List.of(service(null, null, null)), null, PaymentMethod.CASH, "79.99"), CASHIER_ID, BUSINESS_ID))
                    .isInstanceOf(BusinessRuleException.class).hasMessageContaining("less than the total");
        }

        @Test
        void should_throw422_when_stockInsufficient() {
            givenOpenRegisterAndCashier();
            when(productRepository.findById(4L)).thenReturn(Optional.of(shampoo));

            assertThatThrownBy(() -> saleService.createSale(sale(List.of(product(6)), null, PaymentMethod.CASH, null), CASHIER_ID, BUSINESS_ID))
                    .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Insufficient stock");
        }

        @Test
        void should_throw422_when_repeatedLinesExceedStockTogether() {
            givenOpenRegisterAndCashier();
            when(productRepository.findById(4L)).thenReturn(Optional.of(shampoo));

            assertThatThrownBy(() -> saleService.createSale(sale(List.of(product(3), product(3)), null, PaymentMethod.CASH, null), CASHIER_ID, BUSINESS_ID))
                    .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Insufficient stock");
        }
    }

    @Test
    void includedTax_should_roundHalfUp() {
        assertThat(SaleServiceImpl.includedTax(new BigDecimal("245.00"), new BigDecimal("13.00"))).isEqualTo(new BigDecimal("28.19"));
        assertThat(SaleServiceImpl.includedTax(new BigDecimal("100.00"), BigDecimal.ZERO)).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void findAllSales_should_throw404_when_customerUnknown() {
        when(customerRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> saleService.findAllSales(LocalDate.of(2026, 10, 1), null, 99L, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Nested
    class VoidSale {
        private Sale completedSale() {
            var sale = Sale.builder().id(101L).cashRegister(register).cashier(cashier).status(SaleStatus.COMPLETED)
                    .subtotal(new BigDecimal("170.00")).total(new BigDecimal("170.00")).paymentMethod(PaymentMethod.CASH).build();
            sale.addItem(SaleItem.builder().type(ItemType.PRODUCT).itemId(4L).description("Shampoo").quantity(2)
                    .unitPrice(new BigDecimal("85.00")).subtotal(new BigDecimal("170.00")).build());
            return sale;
        }

        @Test
        void should_restoreStockAndMarkVoided_when_registerOpen() {
            var sale = completedSale();
            when(saleRepository.findDetailById(101L)).thenReturn(Optional.of(sale));
            when(cashRegisterRepository.findWithLockByStatus(CashRegisterStatus.OPEN)).thenReturn(Optional.of(register));
            when(productRepository.findById(4L)).thenReturn(Optional.of(shampoo));
            when(userRepository.findById(CASHIER_ID)).thenReturn(Optional.of(cashier));
            when(saleRepository.save(sale)).thenReturn(sale);

            var response = saleService.voidSaleById(101L, new VoidSaleRequest("Cobro duplicado"), CASHIER_ID);

            assertThat(response.status()).isEqualTo(SaleStatus.VOIDED);
            assertThat(response.voidedBy()).isEqualTo("Admin");
            assertThat(response.voidReason()).isEqualTo("Cobro duplicado");
            assertThat(response.voidedAt()).isNotNull();
            assertThat(shampoo.getStock()).isEqualTo(7);
        }

        @Test
        void should_throw422_when_alreadyVoided() {
            var sale = completedSale();
            sale.setStatus(SaleStatus.VOIDED);
            when(saleRepository.findDetailById(101L)).thenReturn(Optional.of(sale));

            assertThatThrownBy(() -> saleService.voidSaleById(101L, new VoidSaleRequest("x"), CASHIER_ID))
                    .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Only completed");
        }

        @Test
        void should_throw422_when_noRegisterOpen() {
            when(saleRepository.findDetailById(101L)).thenReturn(Optional.of(completedSale()));
            when(cashRegisterRepository.findWithLockByStatus(CashRegisterStatus.OPEN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> saleService.voidSaleById(101L, new VoidSaleRequest("x"), CASHIER_ID))
                    .isInstanceOf(BusinessRuleException.class).hasMessageContaining("already closed");
        }

        @Test
        void should_throw422_when_saleBelongsToAnEarlierRegister() {
            when(saleRepository.findDetailById(101L)).thenReturn(Optional.of(completedSale()));
            when(cashRegisterRepository.findWithLockByStatus(CashRegisterStatus.OPEN))
                    .thenReturn(Optional.of(CashRegister.builder().id(16L).build()));

            assertThatThrownBy(() -> saleService.voidSaleById(101L, new VoidSaleRequest("x"), CASHIER_ID))
                    .isInstanceOf(BusinessRuleException.class).hasMessageContaining("already closed");
            assertThat(shampoo.getStock()).isEqualTo(5);
        }

        @Test
        void should_throw404_when_saleMissing() {
            when(saleRepository.findDetailById(101L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> saleService.voidSaleById(101L, new VoidSaleRequest("x"), CASHIER_ID))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
