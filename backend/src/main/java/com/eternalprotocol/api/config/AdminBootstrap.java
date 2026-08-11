package com.eternalprotocol.api.config;

import com.eternalprotocol.api.entity.Admin;
import com.eternalprotocol.api.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Creates exactly one admin login on startup, from ADMIN_BOOTSTRAP_EMAIL /
 * ADMIN_BOOTSTRAP_PASSWORD in .env — and only if the admins table is still
 * empty. This replaces the old DataSeeder, which hardcoded
 * admin@eternalprotocol.com / admin123 directly in source (fine for a
 * throwaway H2 demo, not something you want sitting in git history once
 * you're on a real MySQL database people can actually reach).
 * <p>
 * No sample products are seeded any more either — once you've logged in
 * with the bootstrapped admin, add real products through the admin
 * dashboard UI (Products page → "New product").
 * <p>
 * To promote a second admin later without going through this bootstrap
 * flow again, insert directly into the `admins` table with a bcrypt hash
 * (e.g. generate one via any bcrypt tool, or temporarily set these two env
 * vars again — the check below only skips seeding once an admin already
 * exists, so it's safe to leave the vars in .env permanently).
 */
@Configuration
public class AdminBootstrap {

    @Bean
    public CommandLineRunner bootstrapAdmin(AdminRepository adminRepository,
                                             PasswordEncoder passwordEncoder,
                                             @Value("${app.admin.bootstrap-email:}") String bootstrapEmail,
                                             @Value("${app.admin.bootstrap-password:}") String bootstrapPassword) {
        return args -> {
            if (adminRepository.count() > 0) {
                return; // an admin already exists — never overwrite or duplicate
            }

            if (bootstrapEmail.isBlank() || bootstrapPassword.isBlank()) {
                System.out.println(">>> No admin account exists yet, and ADMIN_BOOTSTRAP_EMAIL / "
                        + "ADMIN_BOOTSTRAP_PASSWORD are not set in .env — skipping admin creation. "
                        + "Set both in .env and restart to create your first admin login.");
                return;
            }

            Admin admin = new Admin();
            admin.setName("Site Admin");
            admin.setEmail(bootstrapEmail);
            admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
            adminRepository.save(admin);
            System.out.println(">>> Created admin login from ADMIN_BOOTSTRAP_EMAIL: " + bootstrapEmail);
        };
    }
}
