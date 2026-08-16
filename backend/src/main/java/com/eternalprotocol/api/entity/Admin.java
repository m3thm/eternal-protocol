package com.eternalprotocol.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Database entity for an admin (staff) account.
 * <p>
 * An "entity" is a plain Java class that Spring Data JPA maps onto a
 * database table. Each field below becomes a column in the {@code admins}
 * table, and each instance of this class represents one row in that table.
 * Admins are the only users allowed to manage products, orders, and
 * athletes through the admin dashboard.
 */
@Entity
@Table(name = "admins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
}
