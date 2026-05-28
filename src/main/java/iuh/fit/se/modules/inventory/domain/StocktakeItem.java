package iuh.fit.se.modules.inventory.domain;

import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inv_stocktake_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StocktakeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private StocktakeSession session;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "system_quantity", nullable = false)
    private int systemQuantity;

    @Column(name = "actual_quantity")
    private Integer actualQuantity;

    @Column(name = "difference_quantity")
    private Integer difference;

    @Column(columnDefinition = "TEXT")
    private String note;

    public static StocktakeItem snapshot(Long bookId, int systemQuantity) {
        return StocktakeItem.builder()
                .bookId(bookId)
                .systemQuantity(systemQuantity)
                .build();
    }

    void attachTo(StocktakeSession session) {
        this.session = session;
    }

    public void recordActualQuantity(int actualQuantity, String note) {
        if (actualQuantity < 0) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Số lượng thực tế không được âm");
        }
        this.actualQuantity = actualQuantity;
        this.difference = actualQuantity - systemQuantity;
        this.note = note;
    }
}
