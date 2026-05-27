package iuh.fit.se.modules.catalog.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "cat_book_review",
        uniqueConstraints = @UniqueConstraint(name = "uk_cat_book_review_book_user", columnNames = {"book_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class BookReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "reviewer_name")
    private String reviewerName;

    @Column(name = "reviewer_email")
    private String reviewerEmail;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "edit_count", nullable = false)
    private int editCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "handling_status", nullable = false, length = 30)
    @Builder.Default
    private ReviewHandlingStatus handlingStatus = ReviewHandlingStatus.NORMAL;

    @Column(name = "issue_type", length = 50)
    private String issueType;

    @Column(name = "admin_public_reply", columnDefinition = "TEXT")
    private String adminPublicReply;

    @Column(name = "admin_replied_at")
    private LocalDateTime adminRepliedAt;

    @Column(name = "support_action", length = 50)
    private String supportAction;

    @Column(name = "flagged_at")
    private LocalDateTime flaggedAt;

    @Column(name = "handled_by_user_id")
    private Long handledByUserId;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void edit(int rating, String content, String reviewerName, String reviewerEmail) {
        if (editCount >= 1) {
            throw new IllegalStateException("Danh gia chi duoc chinh sua 1 lan");
        }
        this.rating = rating;
        this.content = content;
        this.reviewerName = reviewerName;
        this.reviewerEmail = reviewerEmail;
        this.editCount += 1;
        flagIfCritical();
    }

    public void flagIfCritical() {
        if (rating <= 2 && handlingStatus == ReviewHandlingStatus.NORMAL) {
            this.handlingStatus = ReviewHandlingStatus.NEEDS_ACTION;
            this.flaggedAt = LocalDateTime.now();
        }
    }

    public ReviewHandlingStatus updateHandling(
            String issueType,
            String publicReply,
            String supportAction,
            ReviewHandlingStatus nextStatus,
            Long adminUserId
    ) {
        ReviewHandlingStatus previous = this.handlingStatus;
        this.issueType = normalize(issueType);
        String normalizedReply = normalize(publicReply);
        this.adminPublicReply = normalizedReply;
        if (normalizedReply != null) {
            this.adminRepliedAt = LocalDateTime.now();
        }
        this.supportAction = normalize(supportAction);
        this.handlingStatus = nextStatus == null ? ReviewHandlingStatus.IN_PROGRESS : nextStatus;
        this.handledByUserId = adminUserId;
        if (this.handlingStatus == ReviewHandlingStatus.RESOLVED) {
            this.handledAt = LocalDateTime.now();
        }
        return previous;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
