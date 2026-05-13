package iuh.fit.se.modules.admin.application.port.out;

import iuh.fit.se.modules.admin.domain.OrderReport;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderReportPersistencePort {
    long count();

    long countPaidOrders();

    double calculateAverageTimeToPaymentSeconds();

    BigDecimal calculateAverageOrderValue();

    BigDecimal calculateTotalRevenue();

    BigDecimal calculateRefundAmount();

    long countUniqueBuyers();

    Optional<OrderReport> findByOrderId(Long orderId);

    OrderReport save(OrderReport report);

    OrderReport saveAndFlush(OrderReport report);

    int updateStatusToPaidAtomic(Long orderId, String status, Instant paidAt, String paymentMethod);

    List<OrderReport> findPaidReports();
}
