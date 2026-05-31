package iuh.fit.se.modules.payment.adapter.outbound.persistence;

import iuh.fit.se.modules.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);
    Optional<Payment> findByTransactionId(String transactionId);
}
