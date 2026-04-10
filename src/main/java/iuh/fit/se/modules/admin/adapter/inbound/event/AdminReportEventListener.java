package iuh.fit.se.modules.admin.adapter.inbound.event;

import iuh.fit.se.modules.admin.adapter.outbound.persistence.OrderReportRepository;
import iuh.fit.se.modules.admin.domain.OrderReport;
import iuh.fit.se.modules.order.domain.OrderCancelledEvent;
import iuh.fit.se.modules.order.domain.event.OrderCreatedDomainEvent;
import iuh.fit.se.modules.payment.domain.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;



/**
 * AdminReportEventListener — Đồng bộ dữ liệu sang Read Model (OrderReport).
 * Tích hợp CQRS, Transactional Eventual Consistency và State Guard (Principal Standard).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminReportEventListener {

    private final OrderReportRepository repository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderCreated(OrderCreatedDomainEvent event) {
        log.info("📊 CQRS: Synchronizing OrderCreated for {}", event.getOrderId());
        
        try {
            String itemsSummary = event.getItemsSummary();
            if (itemsSummary != null && itemsSummary.length() > 500) {
                itemsSummary = itemsSummary.substring(0, 497) + "...";
            }

            OrderReport report = OrderReport.builder()
                    .orderId(event.getOrderId())
                    .customerName(event.getCustomerName())
                    .totalAmount(event.getTotalAmount())
                    .status("PENDING_PAYMENT")
                    .itemsSummary(itemsSummary)
                    .checkoutAt(event.getOccurredAt())
                    .build();
            
            repository.saveAndFlush(report);
        } catch (DataIntegrityViolationException e) {
            log.warn("OrderReport for {} already exists. Skipping insertion.", event.getOrderId());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        log.info("📊 CQRS: Synchronizing PaymentSuccess for {}", event.getOrderId());
        
        // Sử dụng Native Query với State Guard: Chỉ update nếu status đang là PENDING_PAYMENT
        int updatedRows = repository.updateStatusToPaidAtomic(
                event.getOrderId(), 
                "PAID", 
                event.getOccurredAt(), 
                event.getPaymentMethod()
        );
        
        if (updatedRows == 0) {
            log.warn("⚠️ State Guard Triggered: Order {} status is NOT PENDING_PAYMENT or not found. Update ignored.", event.getOrderId());
        } else {
            log.info("✅ OrderReport for {} marked as PAID via Atomic UPSERT", event.getOrderId());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("📊 CQRS: Synchronizing OrderCancelled for {}", event.getOrderId());
        
        repository.findByOrderId(event.getOrderId()).ifPresent(report -> {
            // State Guard: Chỉ hủy nếu đang PENDING_PAYMENT
            if ("PENDING_PAYMENT".equals(report.getStatus())) {
                report.markCancelled(event.getReason());
                repository.save(report);
                log.info("✅ OrderReport for {} marked as CANCELLED", event.getOrderId());
            } else {
                log.warn("⚠️ Cannot cancel Order {} as its current status is {}", event.getOrderId(), report.getStatus());
            }
        });
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onReturnRefunded(iuh.fit.se.modules.returns.domain.ReturnRequestRefundedEvent event) {
        log.info("📊 CQRS: Synchronizing ReturnRefunded for order {}", event.getOrderId());
        
        repository.findByOrderId(event.getOrderId()).ifPresent(report -> {
            report.markRefunded(event.getRefundAmount(), event.getOccurredAt().atZone(java.time.ZoneId.systemDefault()).toInstant());
            repository.save(report);
            log.info("✅ OrderReport for {} marked as REFUNDED sum: {}", event.getOrderId(), event.getRefundAmount());
        });
    }
}
