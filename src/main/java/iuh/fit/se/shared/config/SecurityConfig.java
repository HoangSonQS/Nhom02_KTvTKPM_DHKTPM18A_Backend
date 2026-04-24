package iuh.fit.se.shared.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cấu hình Spring Security — Stateless JWT.
 * Public endpoints: /api/auth/**, /api-docs/**, /swagger-ui/**
 * Tất cả endpoint còn lại yêu cầu xác thực.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

        @Autowired
        @Qualifier("handlerExceptionResolver")
        private org.springframework.web.servlet.HandlerExceptionResolver resolver;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                // Cấu hình RequestHandler để xử lý CSRF Token từ Header chuẩn hơn (cho
                // SPA/Postman)
                org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler requestHandler = new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler();
                requestHandler.setCsrfRequestAttributeName("_csrf");

                http
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers(
                                                                "/api/v1/auth/login",
                                                                "/api/v1/auth/register",
                                                                "/api/v1/auth/forgot-password",
                                                                "/api/v1/auth/reset-password",
                                                                "/api/v1/auth/refresh")
                                                .csrfTokenRepository(
                                                                org.springframework.security.web.csrf.CookieCsrfTokenRepository
                                                                                .withHttpOnlyFalse())
                                                .csrfTokenRequestHandler(requestHandler))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(exceptions -> exceptions
                                                // Đẩy lỗi 401 về GlobalExceptionHandler xử lý
                                                .authenticationEntryPoint((request, response, authException) -> resolver
                                                                .resolveException(request, response, null,
                                                                                authException))
                                                // Đẩy lỗi 403 về GlobalExceptionHandler xử lý
                                                .accessDeniedHandler(
                                                                (request, response, accessDeniedException) -> resolver
                                                                                .resolveException(request, response,
                                                                                                null,
                                                                                                accessDeniedException)))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/api/v1/auth/login",
                                                                "/api/v1/auth/register",
                                                                "/api/v1/auth/refresh",
                                                                "/api/v1/auth/forgot-password",
                                                                "/api/v1/auth/reset-password",
                                                                "/api/v1/catalog/**",
                                                                "/api/v1/payments/vnpay-ipn",
                                                                "/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/v3/api-docs/**")
                                                .permitAll()
                                                // Logout phải đăng nhập mới cho gọi
                                                .requestMatchers("/api/v1/auth/logout").authenticated()
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(12);
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }
}
