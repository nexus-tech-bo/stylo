package com.example.styloflow.service.impl;

import com.example.styloflow.dto.sale.SaleItemRequest;
import com.example.styloflow.dto.sale.SaleRequest;
import com.example.styloflow.dto.sale.SaleResponse;
import com.example.styloflow.dto.sale.SaleSummaryResponse;
import com.example.styloflow.dto.sale.VoidSaleRequest;
import com.example.styloflow.entity.Sale;
import com.example.styloflow.entity.SaleItem;
import com.example.styloflow.entity.User;
import com.example.styloflow.enums.CashRegisterStatus;
import com.example.styloflow.enums.ItemType;
import com.example.styloflow.enums.PaymentMethod;
import com.example.styloflow.enums.Role;
import com.example.styloflow.enums.SaleStatus;
import com.example.styloflow.exception.BusinessRuleException;
import com.example.styloflow.exception.NotFoundException;
import com.example.styloflow.mapper.SaleMapper;
import com.example.styloflow.repository.BusinessRepository;
import com.example.styloflow.repository.CashRegisterRepository;
import com.example.styloflow.repository.CustomerRepository;
import com.example.styloflow.repository.ProductRepository;
import com.example.styloflow.repository.SaleRepository;
import com.example.styloflow.repository.SalonServiceRepository;
import com.example.styloflow.repository.UserRepository;
import com.example.styloflow.service.SaleService;
import com.example.styloflow.util.DateRange;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SaleServiceImpl implements SaleService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final SaleRepository saleRepository;
    private final CashRegisterRepository cashRegisterRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final ProductRepository productRepository;
    private final BusinessRepository businessRepository;
    private final SaleMapper saleMapper;

    @Override
    @Transactional
    public SaleResponse createSale(SaleRequest request, Long cashierId, Long businessId) {
        // Locks the open register so a concurrent close waits for this sale (and vice versa).
        var register = cashRegisterRepository.findWithLockByStatus(CashRegisterStatus.OPEN)
                .orElseThrow(() -> new BusinessRuleException("There is no open cash register"));
        var sale = Sale.builder()
                .cashRegister(register)
                .cashier(getUserOrThrowById(cashierId))
                .customer(request.customerId() == null ? null : customerRepository.findById(request.customerId())
                        .orElseThrow(() -> new NotFoundException("Customer not found with id: " + request.customerId())))
                .paymentMethod(request.paymentMethod())
                .notes(request.notes())
                .build();

        var subtotal = money(BigDecimal.ZERO);
        for (var line : request.items()) {
            var item = buildItem(line);
            sale.addItem(item);
            subtotal = subtotal.add(item.getSubtotal());
        }

        var discount = money(orZero(request.discount()));
        if (discount.compareTo(subtotal) > 0)
            throw new BusinessRuleException("The discount (" + discount + ") exceeds the subtotal (" + subtotal + ")");
        var total = subtotal.subtract(discount);
        var taxRate = businessRepository.findById(businessId)
                .orElseThrow(() -> new NotFoundException("Business not found with id: " + businessId))
                .getTaxRate();

        sale.setSubtotal(subtotal);
        sale.setDiscount(discount);
        sale.setTotal(total);
        sale.setTax(includedTax(total, taxRate));
        if (request.paymentMethod() == PaymentMethod.CASH) {
            var received = request.amountReceived() != null ? money(request.amountReceived()) : total;
            if (received.compareTo(total) < 0)
                throw new BusinessRuleException("The amount received (" + received + ") is less than the total (" + total + ")");
            sale.setAmountReceived(received);
            sale.setChangeAmount(received.subtract(total));
        } else {
            sale.setAmountReceived(total);
        }
        return saleMapper.toResponse(saleRepository.save(sale));
    }

    @Override
    public Page<SaleSummaryResponse> findAllSales(LocalDate from, LocalDate to, Long customerId, Pageable pageable) {
        var range = DateRange.resolve(from, to);
        if (customerId != null && !customerRepository.existsById(customerId))
            throw new NotFoundException("Customer not found with id: " + customerId);
        return saleRepository.search(range.start(), range.end(), customerId, pageable).map(saleMapper::toSummary);
    }

    @Override
    public SaleResponse findSaleById(Long saleId) {
        return saleMapper.toResponse(getSaleOrThrowById(saleId));
    }

    @Override
    @Transactional
    public SaleResponse voidSaleById(Long saleId, VoidSaleRequest request, Long userId) {
        var sale = getSaleOrThrowById(saleId);
        if (sale.getStatus() != SaleStatus.COMPLETED)
            throw new BusinessRuleException("Only completed sales can be voided");
        // Checked against the locked open register so a concurrent close cannot slip in between.
        var openRegister = cashRegisterRepository.findWithLockByStatus(CashRegisterStatus.OPEN);
        if (openRegister.isEmpty() || !openRegister.get().getId().equals(sale.getCashRegister().getId()))
            throw new BusinessRuleException("The sale's cash register is already closed");

        for (var item : sale.getItems()) {
            if (item.getType() == ItemType.PRODUCT)
                productRepository.findById(item.getItemId())
                        .ifPresent(p -> p.setStock(p.getStock() + item.getQuantity()));
        }
        sale.setStatus(SaleStatus.VOIDED);
        sale.setVoidedBy(getUserOrThrowById(userId));
        sale.setVoidedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        sale.setVoidReason(request.reason());
        return saleMapper.toResponse(saleRepository.save(sale));
    }

    /**
     * Tax included in a tax-inclusive amount: {@code total × rate / (100 + rate)}, HALF_UP to 2 decimals.
     *
     * @param total   tax-inclusive amount
     * @param ratePct tax rate in percent (e.g. 13)
     * @return the tax part of {@code total}
     */
    static BigDecimal includedTax(BigDecimal total, BigDecimal ratePct) {
        return total.multiply(ratePct).divide(HUNDRED.add(ratePct), 2, RoundingMode.HALF_UP);
    }

    /** Validates one line against the catalog, decreases product stock and builds the item with its snapshots. */
    private SaleItem buildItem(SaleItemRequest line) {
        String description;
        BigDecimal catalogPrice;
        if (line.type() == ItemType.SERVICE) {
            var service = salonServiceRepository.findById(line.itemId())
                    .orElseThrow(() -> new NotFoundException("Service not found with id: " + line.itemId()));
            if (!service.isActive())
                throw new BusinessRuleException("The service '" + service.getName() + "' is inactive");
            description = service.getName();
            catalogPrice = service.getPrice();
        } else {
            // Repeated lines of the same product get the same managed instance, so stock is checked cumulatively.
            var product = productRepository.findById(line.itemId())
                    .orElseThrow(() -> new NotFoundException("Product not found with id: " + line.itemId()));
            if (!product.isActive())
                throw new BusinessRuleException("The product '" + product.getName() + "' is inactive");
            if (product.getStock() < line.quantity())
                throw new BusinessRuleException("Insufficient stock for '" + product.getName() + "' (available: " + product.getStock() + ")");
            product.setStock(product.getStock() - line.quantity());
            description = product.getName();
            catalogPrice = product.getPrice();
        }

        var unitPrice = money(line.unitPrice() != null ? line.unitPrice() : catalogPrice);
        var amount = unitPrice.multiply(BigDecimal.valueOf(line.quantity()));
        var discount = money(orZero(line.discount()));
        if (discount.compareTo(amount) > 0)
            throw new BusinessRuleException("The discount of '" + description + "' (" + discount + ") exceeds its amount (" + amount + ")");

        var stylist = line.stylistId() == null ? null : userRepository.findById(line.stylistId())
                .filter(u -> u.isActive() && u.getRole() == Role.STYLIST)
                .orElseThrow(() -> new BusinessRuleException("Invalid stylist with id: " + line.stylistId()));

        return SaleItem.builder()
                .type(line.type())
                .itemId(line.itemId())
                .description(description)
                .quantity(line.quantity())
                .unitPrice(unitPrice)
                .discount(discount)
                .subtotal(amount.subtract(discount))
                .stylist(stylist)
                .commissionRate(stylist != null ? stylist.getCommissionRate() : null)
                .build();
    }

    private Sale getSaleOrThrowById(Long saleId) {
        return saleRepository.findDetailById(saleId)
                .orElseThrow(() -> new NotFoundException("Sale not found with id: " + saleId));
    }

    private User getUserOrThrowById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
