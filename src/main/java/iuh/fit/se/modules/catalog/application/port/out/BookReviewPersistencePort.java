package iuh.fit.se.modules.catalog.application.port.out;

import iuh.fit.se.modules.catalog.domain.BookReview;

import java.util.List;
import java.util.Optional;

public interface BookReviewPersistencePort {

    List<BookReview> findByBookIdOrderByUpdatedAtDesc(Long bookId);

    List<BookReview> findAllByOrderByUpdatedAtDesc();

    Optional<BookReview> findByBookIdAndUserId(Long bookId, Long userId);

    Optional<BookReview> findById(Long id);

    BookReview save(BookReview review);

    void delete(BookReview review);

    double averageRatingByBookId(Long bookId);

    long countByBookId(Long bookId);
}
