package iuh.fit.se.modules.catalog.adapter.outbound.persistence;

import iuh.fit.se.modules.catalog.application.port.out.BookPersistencePort;
import iuh.fit.se.modules.catalog.domain.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * BookPersistenceAdapter — Implement BookPersistencePort.
 */
@Component
@RequiredArgsConstructor
public class BookPersistenceAdapter implements BookPersistencePort {

    private final BookJpaRepository bookJpaRepository;

    @Override
    public Optional<Book> findById(Long id) {
        return bookJpaRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    public Book save(Book book) {
        BookJpaEntity entity = mapToJpa(book);
        BookJpaEntity saved = bookJpaRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public void delete(Long id) {
        bookJpaRepository.deleteById(id);
    }

    @Override
    public List<Book> search(String title, Long categoryId) {
        return bookJpaRepository.search(title, categoryId).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    private Book mapToDomain(BookJpaEntity entity) {
        return Book.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .author(entity.getAuthor())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .quantity(entity.getQuantity())
                .imageUrl(entity.getImageUrl())
                .imagePublicId(entity.getImagePublicId())
                .isActive(entity.isActive())
                .categoryIds(entity.getCategoryIds())
                .build();
    }

    private BookJpaEntity mapToJpa(Book domain) {
        BookJpaEntity entity = bookJpaRepository.findById(domain.getId() != null ? domain.getId() : -1L)
                .orElse(new BookJpaEntity());

        entity.setTitle(domain.getTitle());
        entity.setAuthor(domain.getAuthor());
        entity.setDescription(domain.getDescription());
        entity.setPrice(domain.getPrice());
        entity.setQuantity(domain.getQuantity());
        entity.setImageUrl(domain.getImageUrl());
        entity.setImagePublicId(domain.getImagePublicId());
        entity.setActive(domain.isActive());
        entity.setCategoryIds(domain.getCategoryIds());

        return entity;
    }
}
