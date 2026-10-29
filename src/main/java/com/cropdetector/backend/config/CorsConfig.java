
package com.cropdetector.backend.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource(
        @Value("${app.cors.allowed-origins:${CORS_ALLOWED_ORIGINS:https://frontend-ux-grptask.vercel.app,https://teamtask-frontend-sigma.vercel.app,http://localhost:3000,http://localhost:5173}}")
        String origins
    ) {
        CorsConfiguration config = new CorsConfiguration();

        List<String> allowedList = Arrays.stream(origins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s)
            .distinct()
            .toList();

        config.setAllowedOrigins(allowedList);
        config.setAllowedMethods(
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(
            List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
