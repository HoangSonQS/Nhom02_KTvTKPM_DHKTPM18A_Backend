package iuh.fit.se.modules.notification.adapter.inbound.event;

import io.micrometer.core.instrument.MeterRegistry;
import iuh.fit.se.modules.notification.application.service.NotificationService;
import iuh.fit.se.shared.event.order.OrderCreatedIntegrationEvent;
import iuh.fit.se.shared.event.order.OrderStatusChangedIntegrationEvent;
import iuh.fit.se.shared.event.payment.PaymentSuccessIntegrationEvent;
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
    public void handleOrderCreated(OrderCreatedIntegrationEvent event) {
        setupMdc(event.correlationId());
        try {
            notificationService.processNotification(event.id(), event.orderId(), "ORDER_CREATED", () -> {
                Map<String, Object> vars = Map.of(
                        "customerName", event.customerName(),
                        "orderId", event.orderId(),
                        "totalAmount", event.totalAmount(),
                        "itemsSummary", event.itemsSummary()
                );
                emailPort.sendTemplateEmail(event.customerEmail(), "Xác nhận đơn hàng #" + event.orderId(), "order-confirmation", vars);
            });
        } finally {
            MDC.clear();
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentSuccess(PaymentSuccessIntegrationEvent event) {
        setupMdc(event.correlationId());
        try {
            notificationService.processNotification(event.id(), event.orderId(), "PAYMENT_SUCCESS", () -> {
                log.info("Processing payment success notification for order {}", event.orderId());
                // (Trong thực tế sẽ gọi emailPort tương tự handleOrderCreated)
                meterRegistry.counter("notification.payment.success", "order", event.orderId().toString()).increment();
            });
        } finally {
            MDC.clear();
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderStatusChanged(OrderStatusChangedIntegrationEvent event) {
        setupMdc(event.correlationId());
        try {
            String statusLabel = toStatusLabel(event.toStatus());
            String title = "Đơn hàng #" + event.orderId() + " đã chuyển sang " + statusLabel;
            String message = buildStatusMessage(event, statusLabel);

            notificationService.processCustomerNotification(
                    event.id(),
                    event.orderId(),
                    event.userId(),
                    title,
                    message,
                    "ORDER_STATUS_CHANGED_" + event.toStatus(),
                    () -> {
                        if (!"DELIVERED".equals(event.toStatus())) {
                            log.info("Skip order status email for order {} because status is {}", event.orderId(), event.toStatus());
                            return;
                        }
                        if (event.customerEmail() == null || event.customerEmail().isBlank()) {
                            log.warn("Skip order status email for order {} because customer email is blank", event.orderId());
                            return;
                        }
                        emailPort.sendSimpleEmail(event.customerEmail(), title, message);
                    }
            );
        } finally {
            MDC.clear();
        }
    }

    private void setupMdc(String correlationId) {
        MDC.put("requestId", correlationId);
    }

    private String buildStatusMessage(OrderStatusChangedIntegrationEvent event, String statusLabel) {
        String greeting = event.customerName() == null || event.customerName().isBlank()
                ? "SEBook xin thông báo"
                : "Xin chào " + event.customerName();
        String message = greeting + ", đơn hàng #" + event.orderId() + " của bạn đã được cập nhật sang trạng thái "
                + statusLabel + ".";
        if (event.reason() != null && !event.reason().isBlank()) {
            message += " Lý do: " + event.reason() + ".";
        }
        return message;
    }

    private String toStatusLabel(String status) {
        if (status == null) return "đang cập nhật";
        return switch (status) {
            case "PENDING" -> "chờ xác nhận";
            case "CONFIRMED" -> "đã xác nhận";
            case "PROCESSING" -> "đang xử lý";
            case "DELIVERING" -> "đang giao hàng";
            case "DELIVERED" -> "đã giao hàng";
            case "CANCELLED" -> "đã hủy";
            default -> status;
        };
    }
}
