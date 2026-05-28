package iuh.fit.se.modules.inventory.adapter.outbound.persistence;

import iuh.fit.se.modules.inventory.application.port.out.StocktakePersistencePort;
import iuh.fit.se.modules.inventory.domain.StocktakeSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StocktakePersistenceAdapter implements StocktakePersistencePort {

    private final StocktakeJpaRepository repository;

    @Override
    public StocktakeSession save(StocktakeSession session) {
        return repository.save(session);
    }

    @Override
    public Optional<StocktakeSession> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<StocktakeSession> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public List<StocktakeSession> findByAssignedStaffId(Long staffId) {
        return repository.findByAssignedStaffIdOrderByCreatedAtDesc(staffId);
    }
}
