package com.eternalprotocol.api.config;

import com.eternalprotocol.api.entity.Admin;
import com.eternalprotocol.api.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Creates the very first admin account automatically when the app starts,
 * so there's a way to log in without ever hardcoding a password in the
 * source code.
 * <p>
 * Reads {@code ADMIN_BOOTSTRAP_EMAIL} / {@code ADMIN_BOOTSTRAP_PASSWORD}
 * from the environment (see {@code .env}), and only creates an admin if the
 * {@code admins} table is still completely empty — it will never overwrite
 * or duplicate an existing admin.
 * <p>
 * No sample products are created here. Once logged in with the bootstrapped
 * admin account, add real products through the admin dashboard UI
 * (Products page → "New product").
 * <p>
 * To add a second admin later, either insert a row directly into the
 * {@code admins} table with a bcrypt-hashed password, or simply leave the
 * bootstrap environment variables set — this class only skips creating an
 * admin once one already exists, so it's safe to leave them in {@code .env}
 * permanently.
 */
@Configuration
public class AdminBootstrap {

    /**
     * Registers a {@link CommandLineRunner}, a piece of code Spring Boot
     * automatically runs once, right after the application starts up.
     *
     * @param adminRepository   used to check for and save admin accounts
     * @param passwordEncoder   used to hash the bootstrap password before saving
     * @param bootstrapEmail    admin email from configuration, blank if not set
     * @param bootstrapPassword admin password from configuration, blank if not set
     * @return the startup task that creates the admin account
     */
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
