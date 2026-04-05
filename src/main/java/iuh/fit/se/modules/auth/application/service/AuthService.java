package iuh.fit.se.modules.auth.application.service;

import iuh.fit.se.modules.auth.application.port.in.AuthInternalUseCase;
import iuh.fit.se.modules.auth.application.port.in.AuthUseCase;
import iuh.fit.se.modules.auth.application.port.out.UserPersistencePort;
import iuh.fit.se.modules.auth.application.port.out.RefreshTokenPersistencePort;
import iuh.fit.se.modules.auth.domain.Permission;
import iuh.fit.se.modules.auth.domain.Role;
import iuh.fit.se.modules.auth.domain.User;
import iuh.fit.se.modules.account.application.port.in.AccountInternalUseCase;
import iuh.fit.se.shared.config.JwtTokenProvider;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case implementation cho module auth.
 * Chỉ depends vào domain objects, port/out interfaces, và shared utilities.
 * KHÔNG import bất kỳ class nào từ adapter layer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase, AuthInternalUseCase {

    private final UserPersistencePort userPersistencePort;
    private final AccountInternalUseCase accountInternalUseCase;
    private final RefreshTokenPersistencePort refreshTokenPersistencePort;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserDetailsResponse getUserDetails(Long userId) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_USER_NOT_FOUND));

        return UserDetailsResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional
    public TokenPair login(LoginCommand command, String deviceId) {
        User user = userPersistencePort.findByEmail(command.email())
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        if (!user.isEnabled()) {
            throw new AppException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        // Nếu client không gửi deviceId (lần đầu), tạo mới UUID
        String effectiveDeviceId = (deviceId == null || deviceId.isBlank())
                ? UUID.randomUUID().toString()
                : deviceId;

        // Lấy và tăng rv (Refresh Version) cho thiết bị này
        Integer rv = refreshTokenPersistencePort.incrementAndGetVersion(user.getId().toString(), effectiveDeviceId);

        return generateTokenPair(user, effectiveDeviceId, rv);
    }

    @Override
    @Transactional
    public TokenPair register(RegisterCommand command) {
        if (userPersistencePort.existsByEmail(command.email())) {
            throw new AppException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS);
        }

        User user = User.create(
                command.email(),
                passwordEncoder.encode(command.password()),
                command.fullName(),
                Role.CUSTOMER);

        User saved = userPersistencePort.save(user);

        // Gọi sang module Account thông qua Interface Port/In để tạo profile rỗng
        // (Sync)
        accountInternalUseCase.createDefaultProfile(saved.getId());

        // Đăng ký mặc định cấp deviceId mới
        String deviceId = UUID.randomUUID().toString();
        Integer rv = refreshTokenPersistencePort.incrementAndGetVersion(saved.getId().toString(), deviceId);

        return generateTokenPair(saved, deviceId, rv);
    }

    @Override
    @Transactional
    public TokenPair refreshToken(String refreshToken, String deviceId) {
        if (!jwtTokenProvider.isTokenValid(refreshToken)) {
            throw new AppException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        // 1. Giải mã token để lấy metadata
        var payload = jwtTokenProvider.parseToken(refreshToken);
        String email = payload.getSubject();
        Long userId = payload.get("userId", Long.class);
        Integer rvToken = payload.get("rv", Integer.class);
        String deviceIdFromToken = payload.get("deviceId", String.class);

        // Bảo vệ:deviceId phải khớp
        if (deviceId == null || !deviceId.equals(deviceIdFromToken)) {
            throw new AppException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        // 2. Reuse Detection: Kiểm tra Version với Redis
        Integer rvRedis = refreshTokenPersistencePort.getCurrentVersion(userId.toString(), deviceId);

        if (rvToken == null || !rvToken.equals(rvRedis)) {
            // SECURITY ALERT! Token đã bị dùng lại (version cũ)
            log.warn(
                    "SECURITY ALERT: Token reuse detected! Revoking session. userId={}, deviceId={}, rvToken={}, rvRedis={}",
                    userId, deviceId, rvToken, rvRedis);

            refreshTokenPersistencePort.revokeDeviceSession(userId.toString(), deviceId);
            throw new AppException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        // 3. Rotation: Hợp lệ -> Tăng rv và cấp cặp mới
        User user = userPersistencePort.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_USER_NOT_FOUND));

        Integer nextRv = refreshTokenPersistencePort.incrementAndGetVersion(userId.toString(), deviceId);
        return generateTokenPair(user, deviceId, nextRv);
    }

    @Override
    public void logout(Long userId, String deviceId) {
        if (userId != null && deviceId != null) {
            refreshTokenPersistencePort.revokeDeviceSession(userId.toString(), deviceId);
            log.info("User logged out. userId={}, deviceId={}", userId, deviceId);
        }
    }

    private TokenPair generateTokenPair(User user, String deviceId, Integer rv) {
        // Access Token Claims: Bao gồm Permissions cho RBAC 2.0
        Map<String, Object> accessClaims = new HashMap<>();
        accessClaims.put("userId", user.getId());
        accessClaims.put("role", user.getRole().name());
        accessClaims.put("permissions", user.getRole().getPermissions().stream()
                .map(Permission::name)
                .collect(Collectors.toSet()));

        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), accessClaims);

        // Refresh Token Claims: Bao gồm rv và deviceId
        Map<String, Object> refreshClaims = Map.of(
                "userId", user.getId(),
                "deviceId", deviceId,
                "rv", rv);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), refreshClaims);

        // Lưu vào Redis (Gia hạn TTL)
        refreshTokenPersistencePort.saveRefreshToken(user.getId().toString(), deviceId, refreshToken);

        return new TokenPair(accessToken, refreshToken, deviceId);
    }
}
