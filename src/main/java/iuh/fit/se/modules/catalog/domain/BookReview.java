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
    }
}
