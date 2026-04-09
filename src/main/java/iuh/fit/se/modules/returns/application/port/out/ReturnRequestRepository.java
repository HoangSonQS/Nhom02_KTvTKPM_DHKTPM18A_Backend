package iuh.fit.se.modules.returns.application.port.out;

import iuh.fit.se.modules.returns.domain.ReturnRequest;
import iuh.fit.se.modules.returns.domain.ReturnStatus;

import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository {
    ReturnRequest save(ReturnRequest returnRequest);
    Optional<ReturnRequest> findById(String id);
    List<ReturnRequest> findAll();
    List<ReturnRequest> findByStatus(ReturnStatus status);
    List<ReturnRequest> findByCustomerId(Long customerId);
    List<ReturnRequest> findByOrderId(Long orderId);
}
