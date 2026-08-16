package com.eternalprotocol.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Database entity for a customer — the person placing an order.
 * <p>
 * Maps onto the {@code customers} table. A Customer row is created for
 * every order, whether the buyer has an account or is checking out as a
 * guest. {@code email} and {@code passwordHash} stay optional so guest
 * checkout keeps working without forcing account creation; a "registered"
 * customer is simply a Customer row where those two fields are filled in.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Customer's full name, used for delivery. */
    @Column(nullable = false)
    private String name;

    /** Contact phone number, used for delivery. */
    @Column(nullable = false)
    private String phone;

    /** Delivery address. */
    @Column(nullable = false)
    private String address;

    /** Login email. Optional — guest checkouts don't have one. Must be unique when set. */
    @Column(unique = true)
    private String email;

    /** Bcrypt-hashed password. Optional — only set for registered accounts. */
    @Column(name = "password_hash")
    private String passwordHash;

    /**
     * Creates a Customer with just the required delivery details, used for
     * guest checkout where no account fields are set.
     *
     * @param name    customer's name
     * @param phone   contact number
     * @param address delivery address
     */
    public Customer(String name, String phone, String address) {
        this.name = name;
        this.phone = phone;
        this.address = address;
    }

    /**
     * Whether this customer has a real account (as opposed to a one-off
     * guest checkout).
     *
     * @return true if a password has been set
     */
    public boolean isRegistered() {
        return passwordHash != null;
    }
}
