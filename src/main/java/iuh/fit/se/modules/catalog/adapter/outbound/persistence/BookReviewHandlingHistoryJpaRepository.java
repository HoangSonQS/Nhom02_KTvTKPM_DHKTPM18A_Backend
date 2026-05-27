package iuh.fit.se.modules.catalog.adapter.outbound.persistence;

import iuh.fit.se.modules.catalog.domain.BookReviewHandlingHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookReviewHandlingHistoryJpaRepository extends JpaRepository<BookReviewHandlingHistory, Long> {

    List<BookReviewHandlingHistory> findByReviewIdOrderByCreatedAtDesc(Long reviewId);
}
