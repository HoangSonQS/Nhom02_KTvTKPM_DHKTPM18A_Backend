package iuh.fit.se.modules.inventory.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inv_stock_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StockHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_id", nullable = false, unique = true)
    private String referenceId; // Do caller sinh (OrderId / RequestId)

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockHistoryStatus status;

    @Column(name = "locked_at")
    @CreationTimestamp
    private LocalDateTime lockedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "request_data", columnDefinition = "jsonb")
    private String requestData;

    @Column(name = "result_data", columnDefinition = "jsonb")
    private String resultData;

    public void markSuccess(String resultData) {
        this.status = StockHistoryStatus.SUCCESS;
        this.processedAt = LocalDateTime.now();
        this.resultData = resultData;
    }

    public void markFailed(String resultData) {
        this.status = StockHistoryStatus.FAILED;
        this.processedAt = LocalDateTime.now();
        this.resultData = resultData;
    }
    
    public void markPending() {
        this.status = StockHistoryStatus.PENDING;
        this.lockedAt = LocalDateTime.now();
    }
}
