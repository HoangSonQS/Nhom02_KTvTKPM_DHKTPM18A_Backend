package iuh.fit.se.modules.catalog.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * BookContentJpaEntity — Đại diện bảng cat_book_contents.
 * One-to-One với BookJpaEntity (Shared Primary Key).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cat_book_contents")
public class BookContentJpaEntity {

    @Id
    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "table_of_contents", columnDefinition = "TEXT")
    private String tableOfContents;

    @Column(name = "excerpt", columnDefinition = "TEXT")
    private String excerpt;
}
