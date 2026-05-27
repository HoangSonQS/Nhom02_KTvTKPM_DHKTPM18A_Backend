package iuh.fit.se.modules.catalog.adapter.inbound.web;

import iuh.fit.se.modules.catalog.application.port.in.BookReviewUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import iuh.fit.se.shared.config.UserPrincipal;
import iuh.fit.se.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BookReviewController {

    private final BookReviewUseCase bookReviewUseCase;

    @GetMapping("/api/v1/catalog/books/{bookId}/reviews")
    public ResponseEntity<ApiResponse<List<BookReviewUseCase.BookReviewResponse>>> getBookReviews(@PathVariable Long bookId) {
        return ResponseEntity.ok(ApiResponse.success(bookReviewUseCase.getBookReviews(bookId)));
    }

    @GetMapping("/api/v1/catalog/books/{bookId}/reviews/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookReviewUseCase.BookReviewResponse>> getMyReview(
            @PathVariable Long bookId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(bookReviewUseCase.getMyReview(bookId, currentUserId(authentication))));
    }

    @PostMapping("/api/v1/catalog/books/{bookId}/reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookReviewUseCase.BookReviewResponse>> submitReview(
            @PathVariable Long bookId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        BookReviewUseCase.BookReviewResponse response = bookReviewUseCase.submitReview(
                bookId,
                new BookReviewUseCase.ReviewCommand(
                        currentUserId(authentication),
                        currentFullName(authentication),
                        currentEmail(authentication),
                        request.rating(),
                        request.content(),
                        request.orderId()
                )
        );
        return ResponseEntity.ok(ApiResponse.success("Luu danh gia thanh cong", response));
    }

    @GetMapping("/api/v1/admin/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<BookReviewUseCase.BookReviewResponse>>> getAllReviews() {
        return ResponseEntity.ok(ApiResponse.success(bookReviewUseCase.getAllReviews()));
    }

    @PutMapping("/api/v1/admin/reviews/{id}/handling")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BookReviewUseCase.BookReviewResponse>> updateReviewHandling(
            @PathVariable Long id,
            @RequestBody ReviewHandlingRequest request
    ) {
        BookReviewUseCase.BookReviewResponse response = bookReviewUseCase.updateReviewHandling(
                id,
                new BookReviewUseCase.ReviewHandlingCommand(
                        SecurityUtils.getCurrentUserId(),
                        request.issueType(),
                        request.publicReply(),
                        request.supportAction(),
                        request.status(),
                        request.note()
                )
        );
        return ResponseEntity.ok(ApiResponse.success("Cap nhat xu ly danh gia thanh cong", response));
    }

    @GetMapping("/api/v1/admin/reviews/{id}/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<BookReviewUseCase.ReviewHandlingHistoryResponse>>> getReviewHandlingHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bookReviewUseCase.getReviewHandlingHistory(id)));
    }

    @DeleteMapping("/api/v1/admin/reviews/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
        bookReviewUseCase.deleteReview(id);
        return ResponseEntity.ok(ApiResponse.success("Xoa danh gia thanh cong", null));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication != null && authentication.getCredentials() instanceof Long userId) {
            return userId;
        }
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.userId();
        }
        return null;
    }

    private String currentEmail(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.email();
        }
        return authentication != null ? authentication.getName() : null;
    }

    private String currentFullName(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            Object fullName = principal.claims() != null ? principal.claims().get("fullName") : null;
            if (fullName instanceof String value && !value.isBlank()) {
                return value;
            }
            return principal.email();
        }
        return null;
    }

    public record ReviewRequest(
            @Min(1) @Max(5) int rating,
            String content,
            Long orderId
    ) {}

    public record ReviewHandlingRequest(
            String issueType,
            String publicReply,
            String supportAction,
            String status,
            String note
    ) {}
}
