package iuh.fit.se.modules.catalog.adapter.outbound.persistence;

import iuh.fit.se.modules.catalog.application.port.out.BookReviewPersistencePort;
import iuh.fit.se.modules.catalog.domain.BookReview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BookReviewPersistenceAdapter implements BookReviewPersistencePort {

    private final BookReviewJpaRepository repository;

    @Override
    public List<BookReview> findByBookIdOrderByUpdatedAtDesc(Long bookId) {
        return repository.findByBookIdOrderByUpdatedAtDesc(bookId);
    }

    @Override
    public List<BookReview> findAllByOrderByUpdatedAtDesc() {
        return repository.findAllByOrderByUpdatedAtDesc();
    }

    @Override
    public Optional<BookReview> findByBookIdAndUserId(Long bookId, Long userId) {
        return repository.findByBookIdAndUserId(bookId, userId);
    }

    @Override
    public Optional<BookReview> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public BookReview save(BookReview review) {
        return repository.save(review);
    }

    @Override
    public void delete(BookReview review) {
        repository.delete(review);
    }

    @Override
    public double averageRatingByBookId(Long bookId) {
        return repository.averageRatingByBookId(bookId);
    }

    @Override
    public long countByBookId(Long bookId) {
        return repository.countByBookId(bookId);
    }
}
