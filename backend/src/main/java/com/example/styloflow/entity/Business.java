package com.example.styloflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** A tenant. Lives in {@code public}; its data lives in the schema {@link #schemaName} ({@code tenant_<id>}). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "businesses", schema = "public")
public class Business {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 30)
    private String taxId;

    @Column(length = 200)
    private String address;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "BOB";

    @Column(nullable = false, length = 5)
    @Builder.Default
    private String currencySymbol = "Bs";

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(length = 250)
    private String receiptMessage;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(length = 63, unique = true)
    private String schemaName;

    @PrePersist
    private void prePersist() {
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
