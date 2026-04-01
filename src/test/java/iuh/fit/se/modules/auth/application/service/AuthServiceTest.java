package iuh.fit.se.modules.auth.application.service;

import iuh.fit.se.modules.account.application.port.in.AccountInternalUseCase;
import iuh.fit.se.modules.auth.application.port.in.AuthUseCase;
import iuh.fit.se.modules.auth.application.port.out.UserPersistencePort;
import iuh.fit.se.modules.auth.domain.Role;
import iuh.fit.se.modules.auth.domain.User;
import iuh.fit.se.shared.config.JwtTokenProvider;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho AuthService.
 * Không khởi chạy Spring Context, dùng Mockito để cô lập dependencies.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private AuthService authService;

    @Mock
    private UserPersistencePort userPersistencePort;
    @Mock
    private AccountInternalUseCase accountInternalUseCase;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userPersistencePort, accountInternalUseCase, jwtTokenProvider, passwordEncoder);
    }

    @Test
    void givenValidUser_whenRegister_thenSuccessAndProfileCreated() {
        // Arrange
        var command = new AuthUseCase.RegisterCommand("test@gmail.com", "password123", "Full Name");
        when(userPersistencePort.existsByEmail(command.email())).thenReturn(false);
        when(passwordEncoder.encode(command.password())).thenReturn("encodedPassword");
        
        User savedUser = User.create(command.email(), "encodedPassword", command.fullName(), Role.CUSTOMER);
        savedUser.setId(100L); // Giả lập ID tự tăng
        when(userPersistencePort.save(any(User.class))).thenReturn(savedUser);
        
        when(jwtTokenProvider.generateAccessToken(anyString(), anyMap())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refresh-token");

        // Act
        AuthUseCase.TokenPair result = authService.register(command);

        // Assert
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        
        // Quan trọng: Verify port liên module được gọi (Sync)
        verify(accountInternalUseCase, times(1)).createDefaultProfile(100L);
        verify(userPersistencePort, times(1)).save(any(User.class));
    }

    @Test
    void givenExistingEmail_whenRegister_thenThrowsException() {
        // Arrange
        var command = new AuthUseCase.RegisterCommand("existing@gmail.com", "password", "Name");
        when(userPersistencePort.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(command))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_EMAIL_ALREADY_EXISTS);
        
        verify(accountInternalUseCase, never()).createDefaultProfile(anyLong());
    }

    @Test
    void givenValidCredentials_whenLogin_thenSuccess() {
        // Arrange
        var command = new AuthUseCase.LoginCommand("test@gmail.com", "password123");
        User user = User.create(command.email(), "encodedPassword", "Name", Role.CUSTOMER);
        user.setId(100L);
        
        when(userPersistencePort.findByEmail(command.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(command.password(), user.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(anyString(), anyMap())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refresh");

        // Act
        AuthUseCase.TokenPair result = authService.login(command);

        // Assert
        assertThat(result).isNotNull();
        verify(jwtTokenProvider).generateAccessToken(eq(user.getEmail()), anyMap());
    }
}
