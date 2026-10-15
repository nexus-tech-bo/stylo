package com.example.styloflow.entity;

import com.example.styloflow.enums.CashRegisterStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/** A cash-register session. A partial unique index allows only one {@code OPEN} row per business. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "cash_registers")
public class CashRegister {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private CashRegisterStatus status = CashRegisterStatus.OPEN;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "opened_by_id")
    private User openedBy;

    @Column(nullable = false)
    private Instant openedAt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal openingAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_id")
    private User closedBy;

    private Instant closedAt;

    /** Snapshot taken at close: opening amount + cash sales. {@code null} while open. */
    @Column(precision = 12, scale = 2)
    private BigDecimal expectedCash;

    @Column(precision = 12, scale = 2)
    private BigDecimal countedCash;

    @Column(length = 500)
    private String notes;

    @Version
    private Long version;
}
