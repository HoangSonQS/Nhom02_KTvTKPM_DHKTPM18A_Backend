package iuh.fit.se.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Bật JPA Auditing để @CreatedDate và @LastModifiedDate trong BaseEntity hoạt động.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
