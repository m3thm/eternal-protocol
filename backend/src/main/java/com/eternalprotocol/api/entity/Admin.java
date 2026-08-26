package com.eternalprotocol.api.entity;

import jakarta.persistence.*;

/**
 * Database entity for an admin (staff) account. Admins are the only users
 * allowed to manage products, orders, and athletes through the admin
 * dashboard.
 */
@Entity
@Table(name = "admins")
public class Admin {

    /** Auto-generated primary key, assigned by the database on insert. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display name shown in the admin dashboard. */
    @Column(nullable = false)
    private String name;

    /** Login email. Must be unique across all admins. */
    @Column(unique = true, nullable = false)
    private String email;

    /** Bcrypt-hashed password. Never store or return the raw password. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Default constructor, required by JPA. */
    public Admin() {
    }

    /**
     * Creates a fully-specified admin account.
     *
     * @param name         display name
     * @param email        unique login email
     * @param passwordHash bcrypt-hashed password
     */
    public Admin(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
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
}
