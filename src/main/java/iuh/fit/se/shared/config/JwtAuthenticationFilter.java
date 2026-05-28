package iuh.fit.se.shared.config;

import iuh.fit.se.shared.security.AccessTokenSessionValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads Bearer access tokens, validates the active login session, then sets the SecurityContext.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectProvider<AccessTokenSessionValidator> accessTokenSessionValidatorProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (StringUtils.hasText(token) && jwtTokenProvider.isTokenValid(token)) {
            try {
                var claims = jwtTokenProvider.parseToken(token);
                String subject = claims.getSubject();
                String role = claims.get("role", String.class);
                Long userId = claims.get("userId", Long.class);
                String deviceId = claims.get("deviceId", String.class);
                Integer refreshVersion = claims.get("rv", Integer.class);
                @SuppressWarnings("unchecked")
                List<String> permissions = claims.get("permissions", List.class);

                AccessTokenSessionValidator accessTokenSessionValidator =
                        accessTokenSessionValidatorProvider.getIfAvailable();
                if (accessTokenSessionValidator != null
                        && !accessTokenSessionValidator.isActive(userId, deviceId, refreshVersion)) {
                    log.debug("Rejected access token from inactive session. userId={}, deviceId={}", userId, deviceId);
                    filterChain.doFilter(request, response);
                    return;
                }

                java.util.Set<SimpleGrantedAuthority> authorities = new java.util.HashSet<>();
                if (role != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
                if (permissions != null) {
                    permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
                }

                UserPrincipal principal = new UserPrincipal(subject, claims);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, userId,
                        authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ex) {
                log.debug("Could not set authentication from token: {}", ex.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
