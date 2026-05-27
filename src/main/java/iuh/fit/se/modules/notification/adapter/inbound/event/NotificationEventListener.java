package iuh.fit.se.modules.notification.adapter.inbound.event;

import io.micrometer.core.instrument.MeterRegistry;
import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import iuh.fit.se.modules.notification.application.service.NotificationService;
import iuh.fit.se.shared.event.order.OrderCreatedIntegrationEvent;
import iuh.fit.se.shared.event.order.OrderStatusChangedIntegrationEvent;
import iuh.fit.se.shared.event.payment.PaymentFailedIntegrationEvent;
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
import java.util.Set;

/**
 * NotificationEventListener — Lắng nghe các sự kiện nghiệp vụ để gửi thông báo (Staff+ Standard).
 * Sử dụng TransactionalEventListener để đảm bảo chỉ gửi khi transaction gốc đã COMMIT thành công.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private static final Set<String> ORDER_MANAGEMENT_ROLES = Set.of("ADMIN", "STAFF_SELLER");
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
            notificationService.publishRealtimeToRoles(ORDER_MANAGEMENT_ROLES, new RealtimeEventResponse(
                    "PAYMENT_SUCCESS",
                    event.orderId(),
                    event.userId(),
                    null,
                    null,
                    event.amount(),
                    "SUCCESS",
                    "Đơn hàng #" + event.orderId() + " đã thanh toán thành công",
                    event.occurredAt()
            ));
            notificationService.publishRealtimeToUser(event.userId(), new RealtimeEventResponse(
                    "PAYMENT_SUCCESS",
                    event.orderId(),
                    event.userId(),
                    null,
                    null,
                    event.amount(),
                    "SUCCESS",
                    "Thanh toán đơn hàng #" + event.orderId() + " thành công",
                    event.occurredAt()
            ));
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
    public void handlePaymentFailed(PaymentFailedIntegrationEvent event) {
        setupMdc(event.correlationId());
        try {
            RealtimeEventResponse payload = new RealtimeEventResponse(
                    "PAYMENT_FAILED",
                    event.orderId(),
                    event.userId(),
                    null,
                    null,
                    event.amount(),
                    "FAILED",
                    "Thanh toán đơn hàng #" + event.orderId() + " chưa hoàn tất",
                    event.occurredAt()
            );
            notificationService.publishRealtimeToUser(event.userId(), payload);
            notificationService.publishRealtimeToRoles(ORDER_MANAGEMENT_ROLES, payload);
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
            RealtimeEventResponse payload = new RealtimeEventResponse(
                    "ORDER_STATUS_CHANGED",
                    event.orderId(),
                    event.userId(),
                    null,
                    null,
                    null,
                    event.toStatus(),
                    title,
                    event.occurredAt()
            );
            notificationService.publishRealtimeToUser(event.userId(), payload);
            notificationService.publishRealtimeToRoles(ORDER_MANAGEMENT_ROLES, payload);
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
