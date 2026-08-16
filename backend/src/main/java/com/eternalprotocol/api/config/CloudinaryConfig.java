package com.eternalprotocol.api.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Sets up the connection to Cloudinary, the third-party service that hosts
 * all product images.
 * <p>
 * A {@code @Configuration} class defines "beans" — objects Spring creates
 * once at startup and shares wherever they're needed (here, wherever a
 * {@link Cloudinary} client is injected, e.g. {@code ImageUploadService}).
 */
@Configuration
public class CloudinaryConfig {

    /**
     * Builds the shared Cloudinary client using credentials from
     * configuration (see {@code application.yml} / {@code .env}).
     *
     * @param cloudName Cloudinary account's cloud name
     * @param apiKey    Cloudinary API key
     * @param apiSecret Cloudinary API secret
     * @return a configured Cloudinary client, ready to upload/delete images
     */
    @Bean
    public Cloudinary cloudinary(
            @Value("${app.cloudinary.cloud-name}") String cloudName,
            @Value("${app.cloudinary.api-key}") String apiKey,
            @Value("${app.cloudinary.api-secret}") String apiSecret) {

        Map<String, Object> config = ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true // always return https:// URLs
        );
        return new Cloudinary(config);
    }
}
