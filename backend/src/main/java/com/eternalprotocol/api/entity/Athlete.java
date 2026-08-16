package com.eternalprotocol.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Database entity for an athlete who has a personal discount/referral code.
 * <p>
 * Maps onto the {@code athletes} table. Each athlete has a unique code that
 * customers can enter at checkout: it discounts the customer's order and
 * simultaneously earns the athlete a commission on that same order.
 */
@Entity
@Table(name = "athletes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Athlete {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Athlete's full name. */
    @Column(nullable = false)
    private String name;

    /** Login email. Must be unique across all athletes. */
    @Column(unique = true, nullable = false)
    private String email;

    /** Bcrypt-hashed password. Never store or return the raw password. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Unique code customers enter at checkout (e.g. "RISE45"). */
    @Column(name = "athlete_code", unique = true, nullable = false)
    private String athleteCode;

    /**
     * Percentage used two ways at once: as the discount applied to the
     * customer's order, and as the commission percentage paid to this
     * athlete. For example, 5.00 means both "5% off for the customer" and
     * "5% commission for the athlete" on the same order.
     */
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    /** Whether this athlete's code can currently be used at checkout. */
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
