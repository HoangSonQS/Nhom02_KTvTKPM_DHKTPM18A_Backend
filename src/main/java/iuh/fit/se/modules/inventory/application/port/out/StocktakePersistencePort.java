package iuh.fit.se.modules.inventory.application.port.out;

import iuh.fit.se.modules.inventory.domain.StocktakeSession;

import java.util.List;
import java.util.Optional;

public interface StocktakePersistencePort {
    StocktakeSession save(StocktakeSession session);
    Optional<StocktakeSession> findById(Long id);
    List<StocktakeSession> findAll();
    List<StocktakeSession> findByAssignedStaffId(Long staffId);
}
