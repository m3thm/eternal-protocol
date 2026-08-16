package com.eternalprotocol.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Spring Boot backend.
 * <p>
 * {@code @SpringBootApplication} bundles together component scanning
 * (finding all {@code @Service}, {@code @Repository}, {@code @Controller},
 * etc. classes in this package and below), auto-configuration (Spring
 * setting up sensible defaults based on what's on the classpath, e.g. the
 * database connection), and marks this as the app's configuration root.
 */
@SpringBootApplication
public class EternalProtocolApplication {

    /**
     * Starts the embedded web server and boots the whole Spring
     * application context.
     *
     * @param args command-line arguments, passed through to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(EternalProtocolApplication.class, args);
    }

}
