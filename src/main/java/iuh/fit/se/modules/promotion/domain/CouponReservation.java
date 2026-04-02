package iuh.fit.se.modules.promotion.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "prm_coupon_reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CouponReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "prm_coupon_reservation_seq")
    @SequenceGenerator(name = "prm_coupon_reservation_seq", sequenceName = "prm_coupon_reservation_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "reference_id", nullable = false, unique = true)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void confirm() {
        if (this.status != CouponReservationStatus.RESERVED) {
            throw new IllegalStateException("Chỉ có thể confirm mã đang ở trạng thái RESERVED.");
        }
        this.status = CouponReservationStatus.CONFIRMED;
        this.coupon.incrementUsage();
    }

    public void release() {
        this.status = CouponReservationStatus.RELEASED;
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
