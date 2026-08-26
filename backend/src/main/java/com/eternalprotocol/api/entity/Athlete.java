package com.eternalprotocol.api.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Database entity for an athlete who has a personal discount/referral code.
 * Each athlete's unique code discounts a customer's order and
 * simultaneously earns the athlete a commission on that same order.
 */
@Entity
@Table(name = "athletes")
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
     * Discount/commission percentage — the same figure both discounts the
     * customer's order and pays the athlete's commission on it.
     */
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    /** Whether this athlete's code can currently be used at checkout. */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Default constructor, required by JPA. */
    public Athlete() {
    }

    /**
     * Creates a fully-specified athlete account.
     *
     * @param name           athlete's full name
     * @param email          unique login email
     * @param passwordHash   bcrypt-hashed password
     * @param athleteCode    unique checkout referral code
     * @param commissionRate discount/commission percentage
     * @param active         whether the code is currently usable
     */
    public Athlete(String name, String email, String passwordHash, String athleteCode,
                    BigDecimal commissionRate, boolean active) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.athleteCode = athleteCode;
        this.commissionRate = commissionRate;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getAthleteCode() {
        return athleteCode;
    }

    public void setAthleteCode(String athleteCode) {
        this.athleteCode = athleteCode;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
