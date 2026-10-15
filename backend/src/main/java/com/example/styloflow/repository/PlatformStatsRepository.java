package com.example.styloflow.repository;

import com.example.styloflow.entity.Sale;
import com.example.styloflow.enums.SaleStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;

/** Read-only aggregates for the platform listing; run it inside {@code TenantExecutor.inTenant(schema, ...)}. */
public interface PlatformStatsRepository extends Repository<Sale, Long> {

    @Query("SELECT COUNT(s) AS salesCount, COALESCE(SUM(s.total), 0) AS salesTotal FROM Sale s " +
            "WHERE s.status = :status AND s.createdAt >= :since")
    SalesStats salesSince(@Param("status") SaleStatus status, @Param("since") Instant since);

    interface SalesStats {
        long getSalesCount();

        BigDecimal getSalesTotal();
    }
}
