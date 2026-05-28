package iuh.fit.se.modules.auth.application.service;

import iuh.fit.se.modules.account.application.port.in.AccountInternalUseCase;
import iuh.fit.se.modules.auth.application.port.in.AuthUseCase;
import iuh.fit.se.modules.auth.application.port.out.RefreshTokenPersistencePort;
import iuh.fit.se.modules.auth.application.port.out.UserPersistencePort;
import iuh.fit.se.modules.auth.domain.Role;
import iuh.fit.se.modules.auth.domain.User;
import iuh.fit.se.shared.config.JwtTokenProvider;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho AuthService — Phase 5.5.
 * Kiểm tra xác thực đa thiết bị và cơ chế versioning.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private AuthService authService;

    @Mock
    private UserPersistencePort userPersistencePort;
    @Mock
    private AccountInternalUseCase accountInternalUseCase;
    @Mock
    private RefreshTokenPersistencePort refreshTokenPersistencePort;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userPersistencePort, accountInternalUseCase, refreshTokenPersistencePort, jwtTokenProvider, passwordEncoder, eventPublisher);
    }

    @Test
    void givenValidUser_whenRegister_thenSuccessAndSessionCreated() {
        // Arrange
        var command = new AuthUseCase.RegisterCommand("test@gmail.com", "password123", "Full Name");
        when(userPersistencePort.existsByEmail(command.email())).thenReturn(false);
        when(passwordEncoder.encode(command.password())).thenReturn("encodedPassword");
        
        User savedUser = User.create(command.email(), "encodedPassword", command.fullName(), Role.CUSTOMER);
        savedUser.setId(100L);
        when(userPersistencePort.save(any(User.class))).thenReturn(savedUser);
        
        when(jwtTokenProvider.generateAccessToken(anyString(), anyMap())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(anyString(), anyMap())).thenReturn("refresh-token");
        when(refreshTokenPersistencePort.incrementAndGetVersion(anyString(), anyString())).thenReturn(1);

        // Act
        AuthUseCase.TokenPair result = authService.register(command);

        // Assert
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.deviceId()).isNotNull();
        
        verify(accountInternalUseCase).createDefaultProfile(100L);
        verify(refreshTokenPersistencePort).saveRefreshToken(eq("100"), anyString(), eq("refresh-token"));
    }

    @Test
    void givenValidCredentials_whenLogin_thenSuccessAndVersionIncremented() {
        // Arrange
        var command = new AuthUseCase.LoginCommand("test@gmail.com", "password123");
        String deviceId = "existing-device-id";
        User user = User.create(command.email(), "encodedPassword", "Name", Role.CUSTOMER);
        user.setId(100L);
        
        when(userPersistencePort.findByEmail(command.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(command.password(), user.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(anyString(), anyMap())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken(anyString(), anyMap())).thenReturn("refresh");
        when(refreshTokenPersistencePort.incrementAndGetVersion("100", deviceId)).thenReturn(2);

        // Act
        AuthUseCase.TokenPair result = authService.login(command, deviceId);

        // Assert
        assertThat(result.accessToken()).isEqualTo("token");
        assertThat(result.deviceId()).isEqualTo(deviceId);
        verify(refreshTokenPersistencePort).revokeAllUserSessions("100");
        verify(eventPublisher).publishEvent(any(Object.class));
        verify(refreshTokenPersistencePort).incrementAndGetVersion("100", deviceId);
        verify(refreshTokenPersistencePort).saveRefreshToken("100", deviceId, "refresh");
    }

    @Test
    void givenValidTokenAndCorrectVersion_whenRefresh_thenSuccess() {
        // Arrange
        String refreshToken = "valid-token";
        String deviceId = "device-1";
        Long userId = 100L;
        Integer currentRv = 5;

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("test@gmail.com");
        when(claims.get("userId", Long.class)).thenReturn(userId);
        when(claims.get("rv", Integer.class)).thenReturn(currentRv);
        when(claims.get("deviceId", String.class)).thenReturn(deviceId);

        when(jwtTokenProvider.isTokenValid(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.parseToken(refreshToken)).thenReturn(claims);
        when(refreshTokenPersistencePort.getCurrentVersion(userId.toString(), deviceId)).thenReturn(currentRv);

        User user = User.create("test@gmail.com", "pass", "Name", Role.CUSTOMER);
        user.setId(userId);
        when(userPersistencePort.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(anyString(), anyMap())).thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(anyString(), anyMap())).thenReturn("new-refresh");

        // Act
        AuthUseCase.TokenPair result = authService.refreshToken(refreshToken, deviceId);

        // Assert
        assertThat(result.accessToken()).isEqualTo("new-access");
        verify(refreshTokenPersistencePort).incrementAndGetVersion(userId.toString(), deviceId);
    }

    @Test
    void givenInvalidVersion_whenRefresh_thenSecurityAlertAndRevoke() {
        // Arrange
        String refreshToken = "stolen-token";
        String deviceId = "device-1";
        Long userId = 100L;
        Integer rvFromToken = 3;
        Integer rvInRedis = 4; // Token đã bị refresh trước đó bởi hacker hoặc client lag

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("test@gmail.com");
        when(claims.get("userId", Long.class)).thenReturn(userId);
        when(claims.get("rv", Integer.class)).thenReturn(rvFromToken);
        when(claims.get("deviceId", String.class)).thenReturn(deviceId);

        when(jwtTokenProvider.isTokenValid(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.parseToken(refreshToken)).thenReturn(claims);
        when(refreshTokenPersistencePort.getCurrentVersion(userId.toString(), deviceId)).thenReturn(rvInRedis);

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(refreshToken, deviceId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_TOKEN_INVALID);

        // QUAN TRỌNG: Verify đã revoke session khi phát hiện reuse
        verify(refreshTokenPersistencePort).revokeDeviceSession(userId.toString(), deviceId);
        verify(userPersistencePort, never()).findByEmail(anyString());
    }
}
