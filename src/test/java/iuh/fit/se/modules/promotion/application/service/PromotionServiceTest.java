package iuh.fit.se.modules.promotion.application.service;

import iuh.fit.se.modules.promotion.application.port.out.PromotionPersistencePort;
import iuh.fit.se.modules.promotion.domain.Coupon;
import iuh.fit.se.modules.promotion.domain.CouponReservation;
import iuh.fit.se.modules.promotion.domain.CouponReservationStatus;
import iuh.fit.se.modules.promotion.domain.DiscountType;
import iuh.fit.se.shared.event.realtime.DataChangedRealtimeEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PromotionServiceTest {

    @Test
    void givenReservedCoupon_whenConfirmUsage_thenPublishCouponChanged() {
        PromotionPersistencePort persistencePort = mock(PromotionPersistencePort.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        Coupon coupon = Coupon.builder()
                .code("SAVE10")
                .name("Save 10")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.TEN)
                .usedCount(0)
                .isActive(true)
                .build();
        CouponReservation reservation = CouponReservation.builder()
                .coupon(coupon)
                .referenceId("ORDER-1")
                .status(CouponReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        when(persistencePort.findReservationByReferenceId("ORDER-1")).thenReturn(Optional.of(reservation));
        PromotionService service = new PromotionService(persistencePort, eventPublisher);

        service.confirmCouponUsage("ORDER-1");

        assertThat(reservation.getStatus()).isEqualTo(CouponReservationStatus.CONFIRMED);
        assertThat(coupon.getUsedCount()).isEqualTo(1);
        verify(persistencePort).saveReservation(reservation);
        verify(persistencePort).save(coupon);
        verify(eventPublisher).publishEvent(any(DataChangedRealtimeEvent.class));
    }
}
