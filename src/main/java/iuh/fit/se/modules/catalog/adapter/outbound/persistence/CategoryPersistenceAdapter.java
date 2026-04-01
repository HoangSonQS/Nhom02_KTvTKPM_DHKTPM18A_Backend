package iuh.fit.se.modules.catalog.adapter.outbound.persistence;

import iuh.fit.se.modules.catalog.application.port.out.CategoryPersistencePort;
import iuh.fit.se.modules.catalog.domain.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CategoryPersistenceAdapter — Implement CategoryPersistencePort.
 */
@Component
@RequiredArgsConstructor
public class CategoryPersistenceAdapter implements CategoryPersistencePort {

    private final CategoryJpaRepository categoryJpaRepository;

    @Override
    public Optional<Category> findById(Long id) {
        return categoryJpaRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    public Category save(Category category) {
        CategoryJpaEntity entity = mapToJpa(category);
        CategoryJpaEntity saved = categoryJpaRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public void delete(Long id) {
        categoryJpaRepository.deleteById(id);
    }

    @Override
    public List<Category> findAll() {
        return categoryJpaRepository.findAll().stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByName(String name) {
        return categoryJpaRepository.existsByName(name);
    }

    private Category mapToDomain(CategoryJpaEntity entity) {
        return Category.builder()
                .id(entity.getId())
                .name(entity.getName())
                .isActive(entity.isActive())
                .build();
    }

    private CategoryJpaEntity mapToJpa(Category domain) {
        CategoryJpaEntity entity = categoryJpaRepository.findById(domain.getId() != null ? domain.getId() : -1L)
                .orElse(new CategoryJpaEntity());

        entity.setName(domain.getName());
        entity.setActive(domain.isActive());

        return entity;
    }
}
