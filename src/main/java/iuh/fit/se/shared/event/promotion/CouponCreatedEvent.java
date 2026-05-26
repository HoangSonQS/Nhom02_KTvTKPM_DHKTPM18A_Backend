package iuh.fit.se.shared.event.promotion;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record CouponCreatedEvent(
        UUID eventId,
        Long couponId,
        String code,
        String description,
        String discountType,
        BigDecimal discountValue
) implements Serializable {

    public CouponCreatedEvent(Long couponId, String code, String description, String discountType, BigDecimal discountValue) {
        this(UUID.randomUUID(), couponId, code, description, discountType, discountValue);
    }
}
