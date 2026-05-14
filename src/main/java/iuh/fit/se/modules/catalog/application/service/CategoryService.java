package iuh.fit.se.modules.catalog.application.service;

import iuh.fit.se.modules.catalog.application.port.in.CategoryUseCase;
import iuh.fit.se.modules.catalog.application.port.out.CategoryPersistencePort;
import iuh.fit.se.modules.catalog.domain.Category;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CategoryService — Implementation của CategoryUseCase.
 */
@Service
@RequiredArgsConstructor
public class CategoryService implements CategoryUseCase {

    private final CategoryPersistencePort categoryPersistencePort;

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "STAFF_CREATE_CATEGORY")
    public Category createCategory(String name) {
        if (categoryPersistencePort.existsByName(name)) {
            throw new AppException(ErrorCode.CAT_CATEGORY_ALREADY_EXISTS);
        }
        Category category = Category.builder()
                .name(name)
                .isActive(true)
                .build();
        return categoryPersistencePort.save(category);
    }

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "STAFF_UPDATE_CATEGORY")
    public Category updateCategory(Long id, String newName) {
        Category category = categoryPersistencePort.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy danh mục"));
        
        if (!category.getName().equals(newName) && categoryPersistencePort.existsByName(newName)) {
            throw new AppException(ErrorCode.CAT_CATEGORY_ALREADY_EXISTS);
        }

        category.rename(newName);
        return categoryPersistencePort.save(category);
    }

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "STAFF_DELETE_CATEGORY")
    public void deleteCategory(Long id) {
        categoryPersistencePort.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryPersistencePort.findAll();
    }
}
