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
    private final BookContentJpaRepository bookContentJpaRepository;

    @Override
    public Optional<Book> findById(Long id) {
        return bookJpaRepository.findById(id).map(entity -> {
            BookContentJpaEntity contentEntity = bookContentJpaRepository.findById(id).orElse(null);
            return BookMapper.toDomain(entity, contentEntity);
        });
    }

    @Override
    public boolean existsById(Long id) {
        return bookJpaRepository.existsById(id);
    }

    @Override
    public Book save(Book book) {
        BookJpaEntity existingEntity = book.getId() != null ? 
                bookJpaRepository.findById(book.getId()).orElse(null) : null;
        
        BookJpaEntity entity = BookMapper.toJpa(book, existingEntity);
        BookJpaEntity saved = bookJpaRepository.save(entity);
        
        // Cập nhật ID cho domain nếu là tạo mới để lưu content
        if (book.getId() == null) {
            book.setId(saved.getId());
        }

        // Lưu BookContent nếu có
        if (book.getContent() != null) {
            BookContentJpaEntity contentEntity = BookMapper.toContentJpa(book);
            bookContentJpaRepository.save(contentEntity);
        }

        return findById(saved.getId()).orElseThrow();
    }

    @Override
    public void delete(Long id) {
        bookJpaRepository.deleteById(id);
        // Cascading delete đã được xử lý ở tầng DB (ON DELETE CASCADE)
    }

    @Override
    public List<Book> search(String title, Long categoryId) {
        return bookJpaRepository.search(title, categoryId).stream()
                .map(entity -> {
                    // Để tối ưu hiệu năng, search list không cần load Content ngay lập tức
                    return BookMapper.toDomain(entity, null);
                })
                .collect(Collectors.toList());
    }
}
