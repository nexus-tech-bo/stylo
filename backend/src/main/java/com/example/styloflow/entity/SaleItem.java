package com.example.styloflow.entity;

import com.example.styloflow.enums.ItemType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A sale line. {@code itemId} points to a {@link SalonService} or {@link Product} depending on {@link #type}
 * (no FK); {@link #description} and {@link #commissionRate} are snapshots taken at sale time.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "sale_items")
public class SaleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private Sale sale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ItemType type;

    @Column(nullable = false)
    private Long itemId;

    @Column(nullable = false, length = 120)
    private String description;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    /** unitPrice x quantity - discount. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stylist_id")
    private User stylist;

    /** Stylist's commission rate when the sale was made; {@code null} without stylist. */
    @Column(precision = 5, scale = 2)
    private BigDecimal commissionRate;
}
