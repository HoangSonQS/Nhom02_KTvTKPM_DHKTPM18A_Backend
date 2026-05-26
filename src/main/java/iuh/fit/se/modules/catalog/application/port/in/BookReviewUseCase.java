package iuh.fit.se.modules.catalog.application.port.in;

import java.time.LocalDateTime;
import java.util.List;

public interface BookReviewUseCase {

    List<BookReviewResponse> getBookReviews(Long bookId);

    List<BookReviewResponse> getAllReviews();

    BookReviewResponse getMyReview(Long bookId, Long userId);

    BookReviewResponse submitReview(Long bookId, ReviewCommand command);

    void deleteReview(Long reviewId);

    record ReviewCommand(
            Long userId,
            String reviewerName,
            String reviewerEmail,
            int rating,
            String content
    ) {}

    record BookReviewResponse(
            Long id,
            Long bookId,
            String bookTitle,
            Long userId,
            String reviewerName,
            String reviewerEmail,
            int rating,
            String content,
            int editCount,
            boolean canEdit,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
