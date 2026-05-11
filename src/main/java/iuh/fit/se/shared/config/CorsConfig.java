package iuh.fit.se.shared.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS cho FE (Vite dev: http://localhost:5173).
 * <p>
 * Cho phép gửi cookie (refreshToken HttpOnly) thông qua {@code allowCredentials=true}.
 * Origin và header bổ sung được cấu hình qua property {@code app.cors.allowed-origins}
 * (comma-separated). Mặc định: localhost:5173, localhost:4173 (vite preview).
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}")
    private String allowedOriginsProp;

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOriginsProp.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        cfg.setAllowedOriginPatterns(origins);

        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Device-ID",
                "X-Requested-With",
                "X-CSRF-TOKEN",
                "Accept",
                "Origin"));
        cfg.setExposedHeaders(List.of("Authorization", "X-CSRF-TOKEN"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }
}
