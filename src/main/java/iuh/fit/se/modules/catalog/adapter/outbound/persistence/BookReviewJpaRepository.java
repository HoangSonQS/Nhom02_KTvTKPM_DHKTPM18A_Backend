package iuh.fit.se.modules.catalog.adapter.outbound.persistence;

import iuh.fit.se.modules.catalog.domain.BookReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookReviewJpaRepository extends JpaRepository<BookReview, Long> {

    List<BookReview> findByBookIdOrderByUpdatedAtDesc(Long bookId);

    List<BookReview> findAllByOrderByUpdatedAtDesc();

    Optional<BookReview> findByBookIdAndUserId(Long bookId, Long userId);

    @Query("SELECT COALESCE(AVG(review.rating), 0) FROM BookReview review WHERE review.bookId = :bookId")
    Double averageRatingByBookId(@Param("bookId") Long bookId);

    long countByBookId(Long bookId);
}
