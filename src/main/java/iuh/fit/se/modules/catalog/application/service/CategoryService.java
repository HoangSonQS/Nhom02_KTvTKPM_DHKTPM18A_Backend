package iuh.fit.se.modules.catalog.application.service;

import iuh.fit.se.modules.catalog.application.port.in.CategoryDTO;
import iuh.fit.se.modules.catalog.application.port.in.CategoryUseCase;
import iuh.fit.se.modules.catalog.application.port.out.CategoryPersistencePort;
import iuh.fit.se.modules.catalog.domain.Category;
import iuh.fit.se.shared.event.realtime.AdminDataChangedRealtimeEvent;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

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
        Category savedCategory = categoryPersistencePort.save(category);
        eventPublisher.publishEvent(AdminDataChangedRealtimeEvent.of(
                "CATEGORY",
                "Da them danh muc " + savedCategory.getName()
        ));
        return savedCategory;
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
        Category savedCategory = categoryPersistencePort.save(category);
        eventPublisher.publishEvent(AdminDataChangedRealtimeEvent.of(
                "CATEGORY",
                "Da cap nhat danh muc " + savedCategory.getName()
        ));
        return savedCategory;
    }

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "STAFF_DELETE_CATEGORY")
    public void deleteCategory(Long id) {
        categoryPersistencePort.delete(id);
        eventPublisher.publishEvent(AdminDataChangedRealtimeEvent.of(
                "CATEGORY",
                "Da xoa danh muc #" + id
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryPersistencePort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategorySummaries() {
        return categoryPersistencePort.findAll().stream()
                .map(category -> new CategoryDTO(category.getId(), category.getName(), category.isActive()))
                .toList();
    }
}
