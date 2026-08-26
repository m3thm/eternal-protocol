package com.eternalprotocol.api.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Database entity tracking money owed to an athlete for one order placed
 * with their code.
 * <p>
 * Exactly one Commission row is created per order that used an athlete
 * code, and it moves through the stages in {@link CommissionStatus} as
 * payment is confirmed and later paid out.
 */
@Entity
@Table(name = "commissions")
public class Commission {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The athlete who earns this commission. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    /** The order this commission is for. At most one commission per order. */
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

    /** Default constructor, required by JPA. */
    public Commission() {
    }

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public BigDecimal getCommissionAmount() {
        return commissionAmount;
    }

    public void setCommissionAmount(BigDecimal commissionAmount) {
        this.commissionAmount = commissionAmount;
    }

    public CommissionStatus getCommissionStatus() {
        return commissionStatus;
    }

    public void setCommissionStatus(CommissionStatus commissionStatus) {
        this.commissionStatus = commissionStatus;
    }

    public LocalDateTime getPaidDate() {
        return paidDate;
    }

    public void setPaidDate(LocalDateTime paidDate) {
        this.paidDate = paidDate;
    }
}
