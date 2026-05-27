package iuh.fit.se.modules.returns.adapter.outbound.persistence;

import iuh.fit.se.modules.returns.application.port.out.ReturnRequestRepository;
import iuh.fit.se.modules.returns.domain.ReturnRequest;
import iuh.fit.se.modules.returns.domain.ReturnStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReturnRequestPersistenceAdapter implements ReturnRequestRepository {

    private final JpaReturnRequestRepository jpaRepository;

    @Override
    public ReturnRequest save(ReturnRequest returnRequest) {
        return jpaRepository.save(returnRequest);
    }

    @Override
    public Optional<ReturnRequest> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<ReturnRequest> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<ReturnRequest> findAllNewestFirst() {
        return jpaRepository.findAllByOrderByCreatedAtDescIdDesc();
    }

    @Override
    public List<ReturnRequest> findByStatus(ReturnStatus status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public List<ReturnRequest> findByCustomerId(Long customerId) {
        return jpaRepository.findByCustomerId(customerId);
    }

    @Override
    public List<ReturnRequest> findByCustomerIdNewestFirst(Long customerId) {
        return jpaRepository.findByCustomerIdOrderByCreatedAtDescIdDesc(customerId);
    }

    @Override
    public List<ReturnRequest> findByOrderId(Long orderId) {
        return jpaRepository.findByOrderId(orderId);
    }
}
