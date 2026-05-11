package iuh.fit.se.modules.order.adapter.outbound.persistence;

import iuh.fit.se.modules.order.application.port.out.OrderPersistencePort;
import iuh.fit.se.modules.order.domain.Order;
import iuh.fit.se.modules.order.domain.SagaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderPersistenceAdapter implements OrderPersistencePort {

    private final OrderJpaRepository jpaRepository;

    @Override
    public Order save(Order order) {
        return jpaRepository.save(order);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Order> findByRequestId(String requestId) {
        return jpaRepository.findByRequestId(requestId);
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Optional<Order> findByIdAndUserId(Long id, Long userId) {
        return jpaRepository.findByIdAndUserId(id, userId);
    }

    @Override
    public List<Order> findAbandonedOrders(SagaStatus excludeStatus, LocalDateTime before, int limit) {
        return jpaRepository.findAbandonedOrders(Collections.singletonList(excludeStatus), before, PageRequest.of(0, limit));
    }

    @Override
    public List<Order> findAll() {
        return jpaRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public boolean updateSagaStatusAtomic(Long orderId, SagaStatus currentStatus, SagaStatus nextStatus) {
        return jpaRepository.updateSagaStatusAtomic(orderId, currentStatus, nextStatus) > 0;
    }
}
