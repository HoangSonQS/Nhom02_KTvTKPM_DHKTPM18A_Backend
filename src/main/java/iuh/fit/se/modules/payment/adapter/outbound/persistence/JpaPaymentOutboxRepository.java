package iuh.fit.se.modules.payment.adapter.outbound.persistence;

import iuh.fit.se.modules.payment.domain.PaymentOutboxEvent;
import iuh.fit.se.modules.payment.domain.PaymentOutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaPaymentOutboxRepository extends JpaRepository<PaymentOutboxEvent, String> {
    List<PaymentOutboxEvent> findByStatus(PaymentOutboxStatus status);
}
