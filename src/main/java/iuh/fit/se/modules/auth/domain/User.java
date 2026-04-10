package iuh.fit.se.modules.auth.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * User Aggregate Root — domain thuần túy, không có Spring hay JPA annotation.
 * Được map sang UserJpaEntity ở tầng persistence adapter.
 * Rule: KHÔNG expose class này ra ngoài module auth.
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

    @Setter
    private Long id;
    private String email;
    private String password;
    private String fullName;
    private Role role;
    private boolean enabled;

    /**
     * Factory method để tạo user mới trong quá trình đăng ký.
     * Password phải được mã hóa TRƯỚC khi gọi hàm này.
     */
    public static User create(String email, String encodedPassword,
            String fullName, Role role) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .fullName(fullName)
                .role(role)
                .enabled(true)
                .build();
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
