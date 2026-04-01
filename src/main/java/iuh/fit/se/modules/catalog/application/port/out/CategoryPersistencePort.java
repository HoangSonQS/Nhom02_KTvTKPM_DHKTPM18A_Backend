package iuh.fit.se.modules.catalog.application.port.out;

import iuh.fit.se.modules.catalog.domain.Category;
import java.util.List;
import java.util.Optional;

/**
 * CategoryPersistencePort — Outbound Port cho Category.
 */
public interface CategoryPersistencePort {

    Optional<Category> findById(Long id);

    Category save(Category category);

    void delete(Long id);

    List<Category> findAll();

    boolean existsByName(String name);
}
