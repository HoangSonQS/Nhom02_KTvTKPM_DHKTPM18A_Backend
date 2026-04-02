package iuh.fit.se.shared.infrastructure.email;

import iuh.fit.se.shared.application.port.out.EmailPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * BrevoHealthIndicator — Giám sát trạng thái của Brevo HTTP API cho Actuator.
 * Đảm bảo hệ thống phản ánh đúng sức khỏe của dịch vụ email đang sử dụng (Staff+ Standard).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "brevo.api.key")
public class BrevoHealthIndicator implements HealthIndicator {

    private final EmailPort emailPort;

    @Override
    public Health health() {
        // Nếu EmailPort là implementation thực tế (BrevoEmailAdapter)
        if (emailPort instanceof BrevoEmailAdapter brevoAdapter) {
            try {
                boolean isHealthy = brevoAdapter.isHealthy();
                
                if (isHealthy) {
                    return Health.up()
                            .withDetail("service", "Brevo HTTP API")
                            .withDetail("status", "Reachable")
                            .build();
                } else {
                    return Health.down()
                            .withDetail("service", "Brevo HTTP API")
                            .withDetail("error", "API connection failed or unauthorized")
                            .build();
                }
            } catch (Exception e) {
                return Health.down(e)
                        .withDetail("service", "Brevo")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        }
        
        // Nếu EmailPort bị Mock trong môi trường Test hoặc là implementation khác
        return Health.up()
                .withDetail("service", "Email Service")
                .withDetail("status", "Mocked or custom implementation")
                .build();
    }
}
