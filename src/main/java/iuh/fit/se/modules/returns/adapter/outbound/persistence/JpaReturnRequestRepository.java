package iuh.fit.se.modules.returns.adapter.outbound.persistence;

import iuh.fit.se.modules.returns.domain.ReturnRequest;
import iuh.fit.se.modules.returns.domain.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaReturnRequestRepository extends JpaRepository<ReturnRequest, String> {
    List<ReturnRequest> findByStatus(ReturnStatus status);
    List<ReturnRequest> findByCustomerId(Long customerId);
    List<ReturnRequest> findByOrderId(Long orderId);
}
