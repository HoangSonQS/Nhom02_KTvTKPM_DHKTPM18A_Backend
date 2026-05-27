package iuh.fit.se.modules.catalog.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cat_book_review_handling_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class BookReviewHandlingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", length = 30)
    private String toStatus;

    @Column(name = "issue_type", length = 50)
    private String issueType;

    @Column(name = "public_reply", columnDefinition = "TEXT")
    private String publicReply;

    @Column(name = "support_action", length = 50)
    private String supportAction;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "handled_by_user_id")
    private Long handledByUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
