package com.eternalprotocol.api.config;

import com.eternalprotocol.api.entity.Admin;
import com.eternalprotocol.api.entity.Product;
import com.eternalprotocol.api.repository.AdminRepository;
import com.eternalprotocol.api.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

/**
 * Dev convenience only: seeds one admin login and one sample product so the
 * app is usable immediately after a fresh `mvn spring-boot:run`, without
 * needing to hand-write SQL. Safe to delete before a real deployment, or
 * gate behind a "dev" Spring profile.
 */
@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedData(AdminRepository adminRepository,
                                       ProductRepository productRepository,
                                       PasswordEncoder passwordEncoder) {
        return args -> {
            if (adminRepository.count() == 0) {
                Admin admin = new Admin();
                admin.setName("Site Admin");
                admin.setEmail("admin@eternalprotocol.com");
                admin.setPasswordHash(passwordEncoder.encode("admin123"));
                adminRepository.save(admin);
                System.out.println(">>> Seeded default admin login: admin@eternalprotocol.com / admin123");
            }

            if (productRepository.count() == 0) {
                Product product = new Product();
                product.setStyleCode("PROTOCOL-001");
                product.setName("Protocol 001 Oversized Tee");
                product.setPrice(new BigDecimal("4500.00"));
                product.setColour("Black");
                product.setSize("M");
                product.setStockQty(50);
                product.setImageUrl("https://placehold.co/600x600?text=Protocol+001");
                product.setDescription("Rise Through Discipline. Heavyweight oversized tee, dark premium finish.");
                productRepository.save(product);

                // Second variant of the same style, different colour — so
                // frontend grouping-by-styleCode has something real to group.
                Product variant = new Product();
                variant.setStyleCode("PROTOCOL-001");
                variant.setName("Protocol 001 Oversized Tee");
                variant.setPrice(new BigDecimal("4500.00"));
                variant.setColour("White");
                variant.setSize("M");
                variant.setStockQty(30);
                variant.setImageUrl("https://placehold.co/600x600?text=Protocol+001+White");
                variant.setDescription("Rise Through Discipline. Heavyweight oversized tee, dark premium finish.");
                productRepository.save(variant);

                System.out.println(">>> Seeded sample product: Protocol 001 Oversized Tee (2 variants, styleCode=PROTOCOL-001)");
            }
        };
    }
}
