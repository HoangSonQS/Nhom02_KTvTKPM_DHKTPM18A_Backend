package iuh.fit.se.modules.auth.application.service;

import iuh.fit.se.modules.auth.application.port.in.AuthUseCase;
import iuh.fit.se.modules.auth.application.port.out.UserPersistencePort;
import iuh.fit.se.modules.auth.domain.Role;
import iuh.fit.se.modules.auth.domain.User;
import iuh.fit.se.modules.account.application.port.in.AccountInternalUseCase;
import iuh.fit.se.shared.config.JwtTokenProvider;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Use case implementation cho module auth.
 * Chỉ depends vào domain objects, port/out interfaces, và shared utilities.
 * KHÔNG import bất kỳ class nào từ adapter layer.
 */
@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final UserPersistencePort userPersistencePort;
    private final AccountInternalUseCase accountInternalUseCase;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public TokenPair login(LoginCommand command) {
        User user = userPersistencePort.findByEmail(command.email())
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        if (!user.isEnabled()) {
            throw new AppException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        return generateTokenPair(user);
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
                Role.CUSTOMER
        );

        User saved = userPersistencePort.save(user);

        // Gọi sang module Account thông qua Interface Port/In để tạo profile rỗng (Sync)
        accountInternalUseCase.createDefaultProfile(saved.getId());

        return generateTokenPair(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenPair refreshToken(String refreshToken) {
        if (!jwtTokenProvider.isTokenValid(refreshToken)) {
            throw new AppException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        String email = jwtTokenProvider.getSubject(refreshToken);
        User user = userPersistencePort.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_USER_NOT_FOUND));

        return generateTokenPair(user);
    }

    private TokenPair generateTokenPair(User user) {
        Map<String, Object> claims = Map.of(
                "userId", user.getId(),
                "role", user.getRole().name(),
                "fullName", user.getFullName()
        );
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), claims);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());
        return new TokenPair(accessToken, refreshToken);
    }
}
