package iuh.fit.se.modules.payment.application.port.out;

import iuh.fit.se.modules.payment.domain.Payment;
import java.util.Optional;

public interface PaymentPersistencePort {
    void save(Payment payment);
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findLatestByOrderId(Long orderId);
    Optional<Payment> findByTransactionId(String transactionId);
}
