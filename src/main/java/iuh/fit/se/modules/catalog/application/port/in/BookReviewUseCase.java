package iuh.fit.se.modules.catalog.application.port.in;

import java.time.LocalDateTime;
import java.util.List;

public interface BookReviewUseCase {

    List<BookReviewResponse> getBookReviews(Long bookId);

    List<BookReviewResponse> getAllReviews();

    BookReviewResponse getMyReview(Long bookId, Long userId);

    BookReviewResponse submitReview(Long bookId, ReviewCommand command);

    void deleteReview(Long reviewId);

    BookReviewResponse updateReviewHandling(Long reviewId, ReviewHandlingCommand command);

    List<ReviewHandlingHistoryResponse> getReviewHandlingHistory(Long reviewId);

    record ReviewCommand(
            Long userId,
            String reviewerName,
            String reviewerEmail,
            int rating,
            String content,
            Long orderId
    ) {}

    record ReviewHandlingCommand(
            Long adminUserId,
            String issueType,
            String publicReply,
            String supportAction,
            String status,
            String note
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
            Long orderId,
            int editCount,
            boolean canEdit,
            String handlingStatus,
            String issueType,
            String adminPublicReply,
            LocalDateTime adminRepliedAt,
            String supportAction,
            LocalDateTime flaggedAt,
            Long handledByUserId,
            LocalDateTime handledAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    record ReviewHandlingHistoryResponse(
            Long id,
            Long reviewId,
            String action,
            String fromStatus,
            String toStatus,
            String issueType,
            String publicReply,
            String supportAction,
            String note,
            Long handledByUserId,
            LocalDateTime createdAt
    ) {}
}
