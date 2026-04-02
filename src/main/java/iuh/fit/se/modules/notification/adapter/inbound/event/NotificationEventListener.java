package iuh.fit.se.modules.notification.adapter.inbound.event;

import io.micrometer.core.instrument.MeterRegistry;
import iuh.fit.se.modules.notification.application.service.NotificationService;
import iuh.fit.se.modules.order.domain.OrderCreatedEvent;
import iuh.fit.se.modules.payment.domain.PaymentSuccessEvent;
import iuh.fit.se.shared.application.port.out.EmailPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * NotificationEventListener — Lắng nghe các sự kiện nghiệp vụ để gửi thông báo (Staff+ Standard).
 * Sử dụng TransactionalEventListener để đảm bảo chỉ gửi khi transaction gốc đã COMMIT thành công.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final EmailPort emailPort;
    private final MeterRegistry meterRegistry;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        setupMdc(event.getCorrelationId());
        try {
            notificationService.processNotification(event.getEventId(), event.getOrderId(), "ORDER_CREATED", () -> {
                Map<String, Object> vars = Map.of(
                        "customerName", event.getCustomerName(),
                        "orderId", event.getOrderId(),
                        "totalAmount", event.getTotalAmount(),
                        "itemsSummary", event.getItemsSummary()
                );
                emailPort.sendTemplateEmail(event.getCustomerEmail(), "Xác nhận đơn hàng #" + event.getOrderId(), "order-confirmation", vars);
            });
        } finally {
            MDC.clear();
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        setupMdc(event.getCorrelationId());
        try {
            notificationService.processNotification(event.getEventId(), event.getOrderId(), "PAYMENT_SUCCESS", () -> {
                log.info("Processing payment success notification for order {}", event.getOrderId());
                // (Trong thực tế sẽ gọi emailPort tương tự handleOrderCreated)
                meterRegistry.counter("notification.payment.success", "order", event.getOrderId().toString()).increment();
            });
        } finally {
            MDC.clear();
        }
    }

    private void setupMdc(String correlationId) {
        MDC.put("requestId", correlationId);
    }
}
