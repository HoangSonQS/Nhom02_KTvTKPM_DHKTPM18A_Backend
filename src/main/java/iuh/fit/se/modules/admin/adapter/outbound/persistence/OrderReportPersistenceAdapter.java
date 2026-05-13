package iuh.fit.se.modules.admin.adapter.outbound.persistence;

import iuh.fit.se.modules.admin.application.port.out.OrderReportPersistencePort;
import iuh.fit.se.modules.admin.domain.OrderReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderReportPersistenceAdapter implements OrderReportPersistencePort {

    private final OrderReportRepository repository;

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public long countPaidOrders() {
        return repository.countPaidOrders();
    }

    @Override
    public double calculateAverageTimeToPaymentSeconds() {
        return repository.calculateAverageTimeToPaymentSeconds();
    }

    @Override
    public BigDecimal calculateAverageOrderValue() {
        return repository.calculateAverageOrderValue();
    }

    @Override
    public BigDecimal calculateTotalRevenue() {
        return repository.calculateTotalRevenue();
    }

    @Override
    public BigDecimal calculateRefundAmount() {
        return repository.calculateRefundAmount();
    }

    @Override
    public long countUniqueBuyers() {
        return repository.countUniqueBuyers();
    }

    @Override
    public Optional<OrderReport> findByOrderId(Long orderId) {
        return repository.findByOrderId(orderId);
    }

    @Override
    public OrderReport save(OrderReport report) {
        return repository.save(report);
    }

    @Override
    public OrderReport saveAndFlush(OrderReport report) {
        return repository.saveAndFlush(report);
    }

    @Override
    public int updateStatusToPaidAtomic(Long orderId, String status, Instant paidAt, String paymentMethod) {
        return repository.updateStatusToPaidAtomic(orderId, status, paidAt, paymentMethod);
    }

    @Override
    public List<OrderReport> findPaidReports() {
        return repository.findPaidReports();
    }
}
