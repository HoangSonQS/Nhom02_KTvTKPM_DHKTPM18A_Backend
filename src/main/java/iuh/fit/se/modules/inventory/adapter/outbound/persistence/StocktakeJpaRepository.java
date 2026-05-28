package iuh.fit.se.modules.inventory.adapter.outbound.persistence;

import iuh.fit.se.modules.inventory.domain.StocktakeSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StocktakeJpaRepository extends JpaRepository<StocktakeSession, Long> {
    List<StocktakeSession> findAllByOrderByCreatedAtDesc();
    List<StocktakeSession> findByAssignedStaffIdOrderByCreatedAtDesc(Long assignedStaffId);
}
