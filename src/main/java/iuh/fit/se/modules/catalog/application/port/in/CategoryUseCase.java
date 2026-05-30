package iuh.fit.se.modules.catalog.application.port.in;

import iuh.fit.se.modules.catalog.domain.Category;
import java.util.List;

/**
 * CategoryUseCase — Inbound Port (Public API).
 */
public interface CategoryUseCase {

    Category createCategory(String name);

    Category updateCategory(Long id, String newName);

    void deleteCategory(Long id);

    List<Category> getAllCategories();

    List<CategoryDTO> getAllCategorySummaries();
}
