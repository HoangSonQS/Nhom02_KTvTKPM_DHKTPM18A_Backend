package iuh.fit.se;

import io.jsonwebtoken.Jwts;
import iuh.fit.se.config.GlobalTestConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.security.KeyPair;
import java.util.Base64;

/**
 * Base class cho các Integration Test của Monolith.
 * Tự động tạo cặp khóa RSA 2048-bit tại thời điểm runtime và inject vào Spring context.
 * Giúp Test không phụ thuộc vào file cấu hình vật lý (.pem).
 */
@ActiveProfiles("test")
@Import(GlobalTestConfig.class)
public abstract class BaseIntegrationTest {

    private static final KeyPair TEST_KEY_PAIR;
    private static final String PRIVATE_KEY_B64;
    private static final String PUBLIC_KEY_B64;

    static {
        // Khởi tạo cặp khóa RSA 2048-bit cho môi trường Test
        TEST_KEY_PAIR = Jwts.SIG.RS256.keyPair().build();
        
        PRIVATE_KEY_B64 = Base64.getEncoder().encodeToString(TEST_KEY_PAIR.getPrivate().getEncoded());
        PUBLIC_KEY_B64 = Base64.getEncoder().encodeToString(TEST_KEY_PAIR.getPublic().getEncoded());
    }

    @DynamicPropertySource
    static void configureJwtProperties(DynamicPropertyRegistry registry) {
        // Inject trực tiếp chuỗi Base64 vào Property Registry của Spring
        registry.add("jwt.private-key-source", () -> PRIVATE_KEY_B64);
        registry.add("jwt.public-key-sources", () -> "test-kid:" + PUBLIC_KEY_B64);
        registry.add("jwt.active-kid", () -> "test-kid");
        
        // Mock OTP secret cho test
        registry.add("jwt.otp-secret", () -> "test-otp-secret-key-12345");
    }
}
