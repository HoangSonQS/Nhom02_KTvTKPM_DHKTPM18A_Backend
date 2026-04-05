package iuh.fit.se.modules.logistics.domain;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "log_purchase_order_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PurchaseOrderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "po_id", nullable = false)
    private Long poId;

    @Column(name = "from_status")
    private String fromStatus;

    @Column(name = "to_status", nullable = false)
    private String toStatus;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;

    private String reason;

    public static PurchaseOrderHistory record(Long poId, PurchaseOrderStatus from, PurchaseOrderStatus to, String changedBy, String reason) {
        return PurchaseOrderHistory.builder()
                .poId(poId)
                .fromStatus(from != null ? from.name() : null)
                .toStatus(to.name())
                .changedBy(changedBy)
                .reason(reason)
                .build();
    }
}
