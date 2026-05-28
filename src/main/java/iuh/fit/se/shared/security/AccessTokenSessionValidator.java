package iuh.fit.se.shared.security;

/**
 * Validates that an access token still belongs to the currently active login session.
 */
public interface AccessTokenSessionValidator {
    boolean isActive(Long userId, String deviceId, Integer refreshVersion);
}
