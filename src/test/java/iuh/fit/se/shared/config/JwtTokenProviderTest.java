package iuh.fit.se.shared.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit Test cho JwtTokenProvider.
 * Kiểm tra việc sinh và parse token với thuật toán RS256 và Key Rotation.
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final RsaKeyLoader rsaKeyLoader = Mockito.mock(RsaKeyLoader.class);
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = Jwts.SIG.RS256.keyPair().build();
        
        when(rsaKeyLoader.loadPrivateKey(anyString())).thenReturn(keyPair.getPrivate());
        when(rsaKeyLoader.loadPublicKey(anyString())).thenReturn(keyPair.getPublic());

        jwtTokenProvider = new JwtTokenProvider(rsaKeyLoader);
        
        ReflectionTestUtils.setField(jwtTokenProvider, "privateKeySource", "test-private-key");
        ReflectionTestUtils.setField(jwtTokenProvider, "publicKeySources", "test-kid:test-public-key");
        ReflectionTestUtils.setField(jwtTokenProvider, "activeKid", "test-kid");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiration", 86400000L);
        
        jwtTokenProvider.init();
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
