package iuh.fit.se.shared.config;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test cho JwtTokenProvider.
 * Kiểm tra việc sinh và parse token với custom claims.
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secretKey = "testSecretKeyForThisModuleProjectBackendFitIUHSE2026";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", secretKey);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiration", 86400000L);
    }

    @Test
    void givenEmailAndClaims_whenGenerateToken_thenParseCorrectly() {
        // Arrange
        String email = "test@gmail.com";
        Map<String, Object> claims = Map.of("userId", 123L, "role", "ADMIN");

        // Act
        String token = jwtTokenProvider.generateAccessToken(email, claims);
        Claims parsedClaims = jwtTokenProvider.parseToken(token);

        // Assert
        assertThat(parsedClaims.getSubject()).isEqualTo(email);
        assertThat(parsedClaims.get("userId", Integer.class)).isEqualTo(123); // JJWT map Long to Integer if small
        assertThat(parsedClaims.get("role")).isEqualTo("ADMIN");
        assertThat(jwtTokenProvider.isTokenValid(token)).isTrue();
    }

    @Test
    void givenInvalidToken_whenValidate_thenReturnsFalse() {
        String invalidToken = "this.is.not.a.valid.jwt";
        assertThat(jwtTokenProvider.isTokenValid(invalidToken)).isFalse();
    }
}
