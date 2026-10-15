package com.example.styloflow.repository;

import com.example.styloflow.entity.Sale;
import com.example.styloflow.enums.ItemType;
import com.example.styloflow.enums.PaymentMethod;
import com.example.styloflow.enums.SaleStatus;
import jakarta.persistence.QueryHint;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

/**
 * Read-only aggregate queries for the reports. Every range is {@code createdAt >= start AND createdAt < end}.
 */
public interface ReportRepository extends Repository<Sale, Long> {
    String COMPLETED = "com.example.styloflow.enums.SaleStatus.COMPLETED";
    String VOIDED = "com.example.styloflow.enums.SaleStatus.VOIDED";
    String IN_RANGE = " s.createdAt >= :start AND s.createdAt < :end ";

    interface SalesTotals {
        long getSalesCount();

        BigDecimal getSalesTotal();

        BigDecimal getDiscounts();

        BigDecimal getTax();

        long getVoidedSales();
    }

    interface PaymentMethodTotals {
        PaymentMethod getMethod();

        long getCount();

        BigDecimal getTotal();
    }

    interface DailyTotals {
        /** Day in the business zone as {@code YYYY-MM-DD}. */
        String getDay();

        long getSalesCount();

        BigDecimal getTotal();
    }

    interface StylistTotals {
        Long getStylistId();

        String getStylist();

        long getServices();

        BigDecimal getTotal();

        BigDecimal getCommissionRate();

        BigDecimal getCommission();
    }

    interface TopItem {
        Long getId();

        String getName();

        long getQuantity();

        BigDecimal getTotal();
    }

    interface SaleCsvRow {
        Long getId();

        Instant getCreatedAt();

        String getCashier();

        String getCustomer();

        BigDecimal getSubtotal();

        BigDecimal getDiscount();

        BigDecimal getTotal();

        BigDecimal getTax();

        PaymentMethod getPaymentMethod();

        SaleStatus getStatus();
    }

    @Query("SELECT COUNT(CASE WHEN s.status = " + COMPLETED + " THEN 1 END) AS salesCount, " +
            "SUM(CASE WHEN s.status = " + COMPLETED + " THEN s.total END) AS salesTotal, " +
            "SUM(CASE WHEN s.status = " + COMPLETED + " THEN s.discount END) AS discounts, " +
            "SUM(CASE WHEN s.status = " + COMPLETED + " THEN s.tax END) AS tax, " +
            "COUNT(CASE WHEN s.status = " + VOIDED + " THEN 1 END) AS voidedSales " +
            "FROM Sale s WHERE" + IN_RANGE)
    SalesTotals findSalesTotals(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT SUM(i.discount) FROM SaleItem i JOIN i.sale s WHERE s.status = " + COMPLETED + " AND" + IN_RANGE)
    BigDecimal sumLineDiscounts(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT s.paymentMethod AS method, COUNT(s) AS count, SUM(s.total) AS total " +
            "FROM Sale s WHERE s.status = " + COMPLETED + " AND" + IN_RANGE +
            "GROUP BY s.paymentMethod ORDER BY SUM(s.total) DESC")
    List<PaymentMethodTotals> findTotalsByPaymentMethod(@Param("start") Instant start, @Param("end") Instant end);

    @Query(nativeQuery = true, value = """
            SELECT to_char(s.created_at AT TIME ZONE :zone, 'YYYY-MM-DD') AS "day",
                   COUNT(*) AS "salesCount",
                   SUM(s.total) AS "total"
            FROM sales s
            WHERE s.status = 'COMPLETED' AND s.created_at >= :start AND s.created_at < :end
            GROUP BY 1
            ORDER BY 1""")
    List<DailyTotals> findDailyTotals(@Param("zone") String zone, @Param("start") Instant start, @Param("end") Instant end);

    /** Service lines per stylist; {@code stylistId} {@code null} returns every stylist. */
    @Query("SELECT u.id AS stylistId, u.name AS stylist, COUNT(i) AS services, SUM(i.subtotal) AS total, " +
            "u.commissionRate AS commissionRate, SUM(i.subtotal * COALESCE(i.commissionRate, 0) / 100) AS commission " +
            "FROM SaleItem i JOIN i.stylist u JOIN i.sale s " +
            "WHERE s.status = " + COMPLETED + " AND i.type = com.example.styloflow.enums.ItemType.SERVICE AND" + IN_RANGE +
            "AND (:stylistId IS NULL OR u.id = :stylistId) " +
            "GROUP BY u.id, u.name, u.commissionRate ORDER BY SUM(i.subtotal) DESC, u.id")
    List<StylistTotals> findStylistTotals(@Param("start") Instant start, @Param("end") Instant end,
                                          @Param("stylistId") Long stylistId);

    /** The name is the greatest description snapshot of the item in the range. */
    @Query("SELECT i.itemId AS id, MAX(i.description) AS name, SUM(i.quantity) AS quantity, SUM(i.subtotal) AS total " +
            "FROM SaleItem i JOIN i.sale s " +
            "WHERE s.status = " + COMPLETED + " AND i.type = :type AND" + IN_RANGE +
            "GROUP BY i.itemId ORDER BY SUM(i.quantity) DESC, SUM(i.subtotal) DESC, i.itemId")
    List<TopItem> findTopItems(@Param("type") ItemType type, @Param("start") Instant start, @Param("end") Instant end,
                               Limit limit);

    /** Completed and voided sales, oldest first. Must be consumed and closed inside a transaction. */
    @QueryHints(@QueryHint(name = HibernateHints.HINT_FETCH_SIZE, value = "500"))
    @Query("SELECT s.id AS id, s.createdAt AS createdAt, c.name AS cashier, cu.name AS customer, " +
            "s.subtotal AS subtotal, s.discount AS discount, s.total AS total, s.tax AS tax, " +
            "s.paymentMethod AS paymentMethod, s.status AS status " +
            "FROM Sale s JOIN s.cashier c LEFT JOIN s.customer cu WHERE" + IN_RANGE + "ORDER BY s.id")
    Stream<SaleCsvRow> streamSalesForCsv(@Param("start") Instant start, @Param("end") Instant end);
}
