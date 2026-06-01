package iuh.fit.se.modules.catalog.application.service;

import iuh.fit.se.modules.catalog.application.port.in.BookReviewUseCase;
import iuh.fit.se.modules.catalog.application.port.out.BookPersistencePort;
import iuh.fit.se.modules.catalog.application.port.out.BookReviewPersistencePort;
import iuh.fit.se.modules.catalog.domain.Book;
import iuh.fit.se.modules.catalog.domain.BookReview;
import iuh.fit.se.modules.catalog.domain.BookReviewHandlingHistory;
import iuh.fit.se.modules.catalog.domain.ReviewHandlingStatus;
import iuh.fit.se.shared.event.realtime.ReviewRealtimeEvent;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookReviewService implements BookReviewUseCase {

    private final BookReviewPersistencePort reviewPersistencePort;
    private final BookPersistencePort bookPersistencePort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<BookReviewResponse> getBookReviews(Long bookId) {
        ensureBookExists(bookId);
        return reviewPersistencePort.findByBookIdOrderByUpdatedAtDesc(bookId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookReviewResponse> getAllReviews() {
        return reviewPersistencePort.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookReviewResponse getMyReview(Long bookId, Long userId) {
        if (userId == null) {
            return null;
        }
        return reviewPersistencePort.findByBookIdAndUserId(bookId, userId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "bookDetails", key = "T(iuh.fit.se.shared.cache.CacheKeyUtility).createSaltedKey('bookDetails', #bookId)"),
            @CacheEvict(value = "books", allEntries = true)
    })
    public BookReviewResponse submitReview(Long bookId, ReviewCommand command) {
        validate(bookId, command);
        ensureBookExists(bookId);

        BookReview review = reviewPersistencePort.findByBookIdAndUserId(bookId, command.userId())
                .map(existing -> {
                    try {
                        existing.edit(command.rating(), normalizeContent(command.content()), command.reviewerName(), command.reviewerEmail());
                        return existing;
                    } catch (IllegalStateException exception) {
                        throw new AppException(ErrorCode.INVALID_INPUT, exception.getMessage());
                    }
                })
                .orElseGet(() -> BookReview.builder()
                        .bookId(bookId)
                        .userId(command.userId())
                        .orderId(command.orderId())
                        .reviewerName(command.reviewerName())
                        .reviewerEmail(command.reviewerEmail())
                        .rating(command.rating())
                        .content(normalizeContent(command.content()))
                        .editCount(0)
                        .build());

        review.flagIfCritical();
        BookReview saved = reviewPersistencePort.save(review);
        refreshBookRating(bookId);
        if (saved.getRating() <= 2) {
            reviewPersistencePort.saveHistory(BookReviewHandlingHistory.builder()
                    .reviewId(saved.getId())
                    .action("AUTO_FLAGGED")
                    .fromStatus("NORMAL")
                    .toStatus(saved.getHandlingStatus().name())
                    .note("Hệ thống tự động gắn cờ vì đánh giá 1-2 sao")
                    .build());
        }
        eventPublisher.publishEvent(ReviewRealtimeEvent.changed(
                saved.getId(),
                saved.getBookId(),
                saved.getUserId(),
                saved.getRating(),
                saved.getHandlingStatus().name()
        ));
        return toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"bookDetails", "books"}, allEntries = true)
    public void deleteReview(Long reviewId) {
        BookReview review = reviewPersistencePort.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Khong tim thay danh gia"));
        Long bookId = review.getBookId();
        Long userId = review.getUserId();
        reviewPersistencePort.delete(review);
        refreshBookRating(bookId);
        eventPublisher.publishEvent(ReviewRealtimeEvent.deleted(reviewId, bookId, userId));
    }

    @Override
    @Transactional
    public BookReviewResponse updateReviewHandling(Long reviewId, ReviewHandlingCommand command) {
        BookReview review = reviewPersistencePort.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Khong tim thay danh gia"));
        if (normalizeContent(command.publicReply()) == null) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Noi dung phan hoi khong duoc de trong");
        }

        ReviewHandlingStatus nextStatus = parseStatus(command.status());
        ReviewHandlingStatus previous = review.updateHandling(
                command.issueType(),
                command.publicReply(),
                command.supportAction(),
                nextStatus,
                command.adminUserId()
        );
        BookReview saved = reviewPersistencePort.save(review);
        reviewPersistencePort.saveHistory(BookReviewHandlingHistory.builder()
                .reviewId(saved.getId())
                .action("ADMIN_HANDLED")
                .fromStatus(previous.name())
                .toStatus(saved.getHandlingStatus().name())
                .issueType(saved.getIssueType())
                .publicReply(saved.getAdminPublicReply())
                .supportAction(saved.getSupportAction())
                .note(normalizeContent(command.note()))
                .handledByUserId(command.adminUserId())
                .build());
        eventPublisher.publishEvent(ReviewRealtimeEvent.changed(
                saved.getId(),
                saved.getBookId(),
                saved.getUserId(),
                saved.getRating(),
                saved.getHandlingStatus().name()
        ));
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewHandlingHistoryResponse> getReviewHandlingHistory(Long reviewId) {
        if (reviewPersistencePort.findById(reviewId).isEmpty()) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Khong tim thay danh gia");
        }
        return reviewPersistencePort.findHistoryByReviewId(reviewId).stream()
                .map(history -> new ReviewHandlingHistoryResponse(
                        history.getId(),
                        history.getReviewId(),
                        history.getAction(),
                        history.getFromStatus(),
                        history.getToStatus(),
                        history.getIssueType(),
                        history.getPublicReply(),
                        history.getSupportAction(),
                        history.getNote(),
                        history.getHandledByUserId(),
                        history.getCreatedAt()
                ))
                .toList();
    }

    private void validate(Long bookId, ReviewCommand command) {
        if (bookId == null || command.userId() == null) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Thong tin danh gia khong hop le");
        }
        if (command.rating() < 1 || command.rating() > 5) {
            throw new AppException(ErrorCode.INVALID_INPUT, "So sao danh gia phai tu 1 den 5");
        }
    }

    private void ensureBookExists(Long bookId) {
        if (!bookPersistencePort.existsById(bookId)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Khong tim thay sach");
        }
    }

    private void refreshBookRating(Long bookId) {
        Book book = bookPersistencePort.findById(bookId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Khong tim thay sach"));
        BigDecimal average = BigDecimal.valueOf(reviewPersistencePort.averageRatingByBookId(bookId))
                .setScale(2, RoundingMode.HALF_UP);
        int count = Math.toIntExact(reviewPersistencePort.countByBookId(bookId));
        book.updateRating(average, count);
        bookPersistencePort.save(book);
    }

    private BookReviewResponse toResponse(BookReview review) {
        String bookTitle = bookPersistencePort.findById(review.getBookId())
                .map(Book::getTitle)
                .orElse("SEBook");
        return new BookReviewResponse(
                review.getId(),
                review.getBookId(),
                bookTitle,
                review.getUserId(),
                review.getReviewerName(),
                review.getReviewerEmail(),
                review.getRating(),
                review.getContent(),
                review.getOrderId(),
                review.getEditCount(),
                review.getEditCount() < 1,
                review.getHandlingStatus().name(),
                review.getIssueType(),
                review.getAdminPublicReply(),
                review.getAdminRepliedAt(),
                review.getSupportAction(),
                review.getFlaggedAt(),
                review.getHandledByUserId(),
                review.getHandledAt(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    private ReviewHandlingStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return ReviewHandlingStatus.IN_PROGRESS;
        }
        try {
            return ReviewHandlingStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Trang thai xu ly danh gia khong hop le");
        }
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
