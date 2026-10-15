package com.example.styloflow.repository;

import com.example.styloflow.dto.cashregister.CashRegisterPaymentTotal;
import com.example.styloflow.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    /** A sale with everything {@code SaleResponse} reads: items and their stylists, customer, cashier, register, voider. */
    @EntityGraph(attributePaths = {"items", "items.stylist", "customer", "cashier", "cashRegister", "voidedBy"})
    Optional<Sale> findDetailById(Long id);

    /**
     * Sales created in {@code [start, end)}, optionally of one customer, with what {@code SaleSummaryResponse} reads.
     *
     * @param customerId optional customer filter
     */
    @Query(value = """
            SELECT s FROM Sale s
            JOIN FETCH s.cashier
            JOIN FETCH s.cashRegister
            LEFT JOIN FETCH s.customer c
            WHERE s.createdAt >= :start AND s.createdAt < :end
              AND (:customerId IS NULL OR c.id = :customerId)""",
            countQuery = """
                    SELECT COUNT(s) FROM Sale s
                    LEFT JOIN s.customer c
                    WHERE s.createdAt >= :start AND s.createdAt < :end
                      AND (:customerId IS NULL OR c.id = :customerId)""")
    Page<Sale> search(@Param("start") Instant start, @Param("end") Instant end,
                      @Param("customerId") Long customerId, Pageable pageable);

    /** Count and total of the COMPLETED sales of the given cash registers, grouped by register and payment method. */
    @Query("""
            SELECT new com.example.styloflow.dto.cashregister.CashRegisterPaymentTotal(
                       s.cashRegister.id, s.paymentMethod, COUNT(s), SUM(s.total))
            FROM Sale s
            WHERE s.cashRegister.id IN :cashRegisterIds
              AND s.status = com.example.styloflow.enums.SaleStatus.COMPLETED
            GROUP BY s.cashRegister.id, s.paymentMethod""")
    List<CashRegisterPaymentTotal> sumCompletedByPaymentMethod(@Param("cashRegisterIds") Collection<Long> cashRegisterIds);
}
