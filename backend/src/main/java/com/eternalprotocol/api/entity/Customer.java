package com.eternalprotocol.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A customer row is created for every order — guest or registered.
 * email/passwordHash stay nullable so guest checkout (no account) keeps
 * working exactly as it did before login existed; a "registered" customer
 * is just a Customer row where those two fields happen to be set.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String address;

    @Column(unique = true)
    private String email; // nullable — guest checkouts won't have one

    @Column(name = "password_hash")
    private String passwordHash; // nullable — same reason

    public Customer(String name, String phone, String address) {
        this.name = name;
        this.phone = phone;
        this.address = address;
    }

    public boolean isRegistered() {
        return passwordHash != null;
    }
}
