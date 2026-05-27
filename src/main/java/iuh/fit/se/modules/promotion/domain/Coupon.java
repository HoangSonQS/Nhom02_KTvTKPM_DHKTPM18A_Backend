package iuh.fit.se.modules.promotion.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prm_coupon")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    private BigDecimal discountValue;

    @Column(name = "min_order_value")
    private BigDecimal minOrderValue;

    @Column(name = "max_discount_value")
    private BigDecimal maxDiscountValue;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isAppliable(BigDecimal orderTotal) {
        if (!isActive) return false;
        
        LocalDateTime now = LocalDateTime.now();
        if (startDate != null && now.isBefore(startDate)) return false;
        if (endDate != null && now.isAfter(endDate)) return false;

        if (usageLimit != null && usedCount >= usageLimit) return false;

        if (minOrderValue != null && orderTotal.compareTo(minOrderValue) < 0) return false;

        return true;
    }

    public BigDecimal calculateDiscount(BigDecimal orderTotal) {
        if (!isAppliable(orderTotal)) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (discountType == DiscountType.FIXED_AMOUNT) {
            discount = discountValue;
        } else if (discountType == DiscountType.PERCENTAGE) {
            // value is e.g. 10 for 10%
            discount = orderTotal.multiply(discountValue).divide(BigDecimal.valueOf(100));
        }

        // Apply max discount cap
        if (maxDiscountValue != null && discount.compareTo(maxDiscountValue) > 0) {
            discount = maxDiscountValue;
        }

        // Discount cannot exceed order total
        if (discount.compareTo(orderTotal) > 0) {
            discount = orderTotal;
        }

        return discount;
    }

    public void incrementUsage() {
        if (usageLimit != null && usedCount >= usageLimit) {
            throw new IllegalStateException("Đã đạt giới hạn sử dụng mã khuyến mãi.");
        }
        this.usedCount++;
    }

    /** Admin update — chỉ cho phép sửa các trường mutable, không đổi được code hay discountType. */
    public void update(String name, String description, java.math.BigDecimal discountValue,
                       java.math.BigDecimal minOrderValue, java.math.BigDecimal maxDiscountValue,
                       Integer usageLimit, LocalDateTime startDate, LocalDateTime endDate, boolean isActive) {
        this.name = name;
        this.description = description;
        this.discountValue = discountValue;
        this.minOrderValue = minOrderValue;
        this.maxDiscountValue = maxDiscountValue;
        this.usageLimit = usageLimit;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive;
    }
}
