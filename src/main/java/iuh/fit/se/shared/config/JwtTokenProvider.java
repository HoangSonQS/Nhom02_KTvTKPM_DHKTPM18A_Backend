package iuh.fit.se.shared.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tiện ích tạo và xác thực JWT token sử dụng thuật toán RS256.
 * Hỗ trợ Key Rotation (kid) và Anti-spoofing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final RsaKeyLoader rsaKeyLoader;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${jwt.private-key-source:}")
    private String privateKeySource;

    @Value("${jwt.public-key-sources:}")
    private String publicKeySources; // Định dạng: kid1:source1,kid2:source2

    @Value("${jwt.active-kid:}")
    private String activeKid;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private PrivateKey privateKey;
    private final Map<String, PublicKey> publicKeys = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            log.info("Initializing RS256 JWT keys (Key Rotation enabled)...");

            // Load Private Key for signing
            if (privateKeySource != null && !privateKeySource.isEmpty()) {
                this.privateKey = rsaKeyLoader.loadPrivateKey(privateKeySource);
                log.info("Loaded Private Key successfully.");
            }

            // Load Public Keys for verification
            if (publicKeySources != null && !publicKeySources.isEmpty()) {
                String[] sources = publicKeySources.split(",");
                for (String source : sources) {
                    String[] parts = source.split(":", 2);
                    if (parts.length == 2) {
                        String kid = parts[0].trim();
                        String pathOrBase64 = parts[1].trim();
                        publicKeys.put(kid, rsaKeyLoader.loadPublicKey(pathOrBase64));
                        log.info("Loaded Public Key for kid: {}", kid);
                    }
                }
            }

            // --- STARTUP GUARDS (Phase 9 Hardening) ---
            if (this.privateKey == null) {
                log.error("CRITICAL: Private key is missing! JWT signing will fail.");
                throw new IllegalStateException("JWT Private Key must be configured (jwt.private-key-source)");
            }

            if (this.publicKeys.isEmpty()) {
                log.error("CRITICAL: No public keys loaded! JWT verification will fail.");
                throw new IllegalStateException("At least one JWT Public Key must be configured (jwt.public-key-sources)");
            }

            if (activeKid == null || activeKid.isEmpty() || !publicKeys.containsKey(activeKid)) {
                log.error("CRITICAL: Active kid '{}' not found in loaded public keys.", activeKid);
                throw new IllegalStateException("The current active-kid must exist in public-key-sources");
            }

            log.info("RS256 Security Context initialized successfully with {} verify keys.", publicKeys.size());

        } catch (IllegalStateException e) {
            throw e; // Reraise guards
        } catch (Exception e) {
            log.error("Failed to initialize RSA keys for JWT", e);
            throw new RuntimeException("Could not initialize JWT keys due to unexpected error", e);
        }
    }

    public String generateAccessToken(String subject, Map<String, Object> claims) {
        return Jwts.builder()
                .header().add("kid", activeKid).and()
                .subject(subject)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public String generateRefreshToken(String subject, Map<String, Object> claims) {
        return Jwts.builder()
                .header().add("kid", activeKid).and()
                .subject(subject)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            // 1. Manually extract kid and check alg (Anti-spoofing)
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new JwtException("Malformed JWT");
            }

            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode headerNode = objectMapper.readTree(headerJson);
            
            String alg = headerNode.has("alg") ? headerNode.get("alg").asText() : null;
            String kid = headerNode.has("kid") ? headerNode.get("kid").asText() : null;

            if (!"RS256".equals(alg)) {
                log.warn("Security Alert: Blocked JWT with alg: {}", alg);
                throw new JwtException("Invalid algorithm");
            }

            if (kid == null || !publicKeys.containsKey(kid)) {
                throw new JwtException("Unknown or missing Key ID (kid)");
            }

            // 2. Verify and parse using the correct Public Key
            return Jwts.parser()
                    .verifyWith(publicKeys.get(kid))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (Exception e) {
            log.debug("JWT parse failed: {}", e.getMessage());
            throw new JwtException("Authentication failed", e);
        }
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getSubject(String token) {
        return parseToken(token).getSubject();
    }
}
