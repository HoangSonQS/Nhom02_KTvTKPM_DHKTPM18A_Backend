package iuh.fit.se.modules.logistics.application.port.out;

import iuh.fit.se.modules.logistics.domain.Supplier;
import java.util.List;
import java.util.Optional;

public interface SupplierPersistencePort {
    Supplier save(Supplier supplier);
    Optional<Supplier> findById(Long id);
    List<Supplier> findAll();
}
