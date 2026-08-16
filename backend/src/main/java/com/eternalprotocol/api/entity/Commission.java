package com.eternalprotocol.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Database entity tracking money owed to an athlete for one order placed
 * with their code.
 * <p>
 * Maps onto the {@code commissions} table. Exactly one Commission row is
 * created per order that used an athlete code, and it moves through the
 * stages described in {@link CommissionStatus} as payment is confirmed and
 * later paid out to the athlete.
 */
@Entity
@Table(name = "commissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Commission {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The athlete who earns this commission. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    /**
     * The order this commission is for. {@code @OneToOne} plus
     * {@code unique = true} means each order can have at most one
     * commission — never two.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    /** Amount owed to the athlete, calculated from the order total and the athlete's commission rate. */
    @Column(name = "commission_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionAmount;

    /** Current stage of this commission — see {@link CommissionStatus}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "commission_status", nullable = false)
    private CommissionStatus commissionStatus = CommissionStatus.AWAITING_PAYMENT;

    /** Date the admin marked this commission as paid out. Null until then. */
    @Column(name = "paid_date")
    private LocalDateTime paidDate;

    /**
     * Creates a new commission in the {@code AWAITING_PAYMENT} status.
     *
     * @param athlete           athlete earning the commission
     * @param order             the order the commission is calculated from
     * @param commissionAmount  amount owed to the athlete
     */
    public Commission(Athlete athlete, Order order, BigDecimal commissionAmount) {
        this.athlete = athlete;
        this.order = order;
        this.commissionAmount = commissionAmount;
        this.commissionStatus = CommissionStatus.AWAITING_PAYMENT;
    }
}
