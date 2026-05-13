package iuh.fit.se.modules.admin.adapter.inbound.event;

import iuh.fit.se.modules.admin.application.port.out.OrderReportPersistencePort;
import iuh.fit.se.modules.admin.domain.OrderReport;
import iuh.fit.se.modules.order.domain.OrderCancelledEvent;
import iuh.fit.se.modules.order.domain.event.OrderCreatedDomainEvent;
import iuh.fit.se.modules.order.domain.event.OrderFulfillmentStatusChangedEvent;
import iuh.fit.se.modules.payment.domain.PaymentSuccessEvent;
import iuh.fit.se.modules.returns.domain.ReturnRequest;
import iuh.fit.se.modules.returns.domain.event.ReturnDomainEvents.ReturnRequestRefundedDomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminReportEventListener {

    private final OrderReportPersistencePort repository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Caching(evict = @CacheEvict(value = "dashboardStats", allEntries = true))
    public void onOrderCreated(OrderCreatedDomainEvent event) {
        log.info("CQRS: Synchronizing OrderCreated for {}", event.getOrderId());

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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Caching(evict = @CacheEvict(value = "dashboardStats", allEntries = true))
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        log.info("CQRS: Synchronizing PaymentSuccess for {}", event.getOrderId());

        int updatedRows = repository.updateStatusToPaidAtomic(
                event.getOrderId(),
                "CONFIRMED",
                event.getOccurredAt(),
                event.getPaymentMethod()
        );

        if (updatedRows == 0) {
            log.warn("State guard: Order {} status is not PENDING_PAYMENT or not found.", event.getOrderId());
        } else {
            log.info("OrderReport for {} marked as CONFIRMED via atomic update.", event.getOrderId());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Caching(evict = @CacheEvict(value = "dashboardStats", allEntries = true))
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("CQRS: Synchronizing OrderCancelled for {}", event.getOrderId());

        repository.findByOrderId(event.getOrderId()).ifPresent(report -> {
            if ("PENDING_PAYMENT".equals(report.getStatus())) {
                report.markCancelled(event.getReason());
                repository.save(report);
                log.info("OrderReport for {} marked as CANCELLED.", event.getOrderId());
            } else {
                log.warn("Cannot cancel OrderReport {} because current status is {}.", event.getOrderId(), report.getStatus());
            }
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Caching(evict = @CacheEvict(value = "dashboardStats", allEntries = true))
    public void onOrderFulfillmentStatusChanged(OrderFulfillmentStatusChangedEvent event) {
        log.info("CQRS: Synchronizing OrderFulfillmentStatusChanged for {} to {}",
                event.getOrderId(), event.getToStatus());

        repository.findByOrderId(event.getOrderId()).ifPresent(report -> {
            report.markFulfillmentStatus(
                    event.getToStatus().name(),
                    event.getOccurredAt(),
                    event.getReason()
            );
            repository.save(report);
            log.info("OrderReport for {} marked as {}.", event.getOrderId(), event.getToStatus());
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Caching(evict = @CacheEvict(value = "dashboardStats", allEntries = true))
    public void onReturnRefunded(ReturnRequestRefundedDomainEvent event) {
        ReturnRequest request = event.getReturnRequest();
        log.info("CQRS: Synchronizing ReturnRefunded for order {}", request.getOrderId());

        repository.findByOrderId(request.getOrderId()).ifPresent(report -> {
            report.markRefunded(request.getRefundAmount(), event.getOccurredAt());
            repository.save(report);
            log.info("OrderReport for {} marked as REFUNDED sum: {}", request.getOrderId(), request.getRefundAmount());
        });
    }
}
