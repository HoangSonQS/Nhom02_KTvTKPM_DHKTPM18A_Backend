package iuh.fit.se.modules.auth.adapter.outbound.persistence;

import iuh.fit.se.shared.domain.BaseEntity;
import iuh.fit.se.modules.auth.domain.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity cho bảng auth_user.
 * Đây là class duy nhất trong module auth được dùng annotations JPA/Spring.
 * KHÔNG được expose ra ngoài adapter/outbound/persistence package.
 * Naming Convention: prefix "auth_" (rule từ project-rules-be.md)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auth_user")
public class UserJpaEntity extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;
}
