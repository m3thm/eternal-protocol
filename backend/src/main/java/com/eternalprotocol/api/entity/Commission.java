package com.eternalprotocol.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Commission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "commission_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_status", nullable = false)
    private CommissionStatus commissionStatus = CommissionStatus.AWAITING_PAYMENT;

    @Column(name = "paid_date")
    private LocalDateTime paidDate;

    public Commission(Athlete athlete, Order order, BigDecimal commissionAmount) {
        this.athlete = athlete;
        this.order = order;
        this.commissionAmount = commissionAmount;
        this.commissionStatus = CommissionStatus.AWAITING_PAYMENT;
    }
}
