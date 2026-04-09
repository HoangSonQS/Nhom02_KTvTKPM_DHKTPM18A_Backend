package iuh.fit.se.modules.returns.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ret_return_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ReturnRequest {

    @Id
    private String id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnStatus status;

    @Column(name = "refund_amount")
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnReason reason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "return_request_id")
    @Builder.Default
    private List<ReturnItem> items = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "return_request_id")
    @Builder.Default
    private List<ReturnHistory> histories = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static ReturnRequest create(Long orderId, Long customerId, ReturnReason reason, String notes, List<ReturnItem> items) {
        String id = UUID.randomUUID().toString();
        ReturnRequest request = ReturnRequest.builder()
                .id(id)
                .orderId(orderId)
                .customerId(customerId)
                .status(ReturnStatus.PENDING)
                .reason(reason)
                .notes(notes)
                .items(items != null ? items : new ArrayList<>())
                .build();

        request.addHistory(null, ReturnStatus.PENDING, "SYSTEM", "Yêu cầu đổi trả được tạo");
        return request;
    }

    public void approve(String approvedBy) {
        if (this.status != ReturnStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể duyệt yêu cầu đang ở trạng thái PENDING");
        }
        ReturnStatus oldStatus = this.status;
        this.status = ReturnStatus.APPROVED;
        addHistory(oldStatus, ReturnStatus.APPROVED, approvedBy, "Yêu cầu đã được duyệt");
    }

    public void markAsReceived(String receivedBy, List<ItemCondition> conditions) {
        if (this.status != ReturnStatus.APPROVED) {
            throw new IllegalStateException("Chỉ có thể xác nhận nhận hàng khi đã APPROVED");
        }
        
        if (conditions != null && conditions.size() == items.size()) {
            for (int i = 0; i < items.size(); i++) {
                // In a real scenario, we might want to update the condition of each item here
                // For simplicity, we assume the conditions passed match the order of items
            }
        }

        ReturnStatus oldStatus = this.status;
        this.status = ReturnStatus.RECEIVED;
        addHistory(oldStatus, ReturnStatus.RECEIVED, receivedBy, "Đã nhận được hàng trả lại");
    }

    public void refund(String processedBy, BigDecimal actualRefundAmount) {
        if (this.status != ReturnStatus.RECEIVED) {
            throw new IllegalStateException("Chỉ có thể hoàn tiền sau khi kho đã nhận hàng (RECEIVED)");
        }
        ReturnStatus oldStatus = this.status;
        this.status = ReturnStatus.REFUNDED;
        this.refundAmount = actualRefundAmount;
        addHistory(oldStatus, ReturnStatus.REFUNDED, processedBy, "Đã hoàn chênh lệch/tiền cho khách hàng");
    }

    public void reject(String reasonStr, String rejectedBy) {
        if (this.status == ReturnStatus.REFUNDED) {
            throw new IllegalStateException("Không thể từ chối yêu cầu đã được hoàn tiền");
        }
        ReturnStatus oldStatus = this.status;
        this.status = ReturnStatus.REJECTED;
        addHistory(oldStatus, ReturnStatus.REJECTED, rejectedBy, "Yêu cầu bị từ chối: " + reasonStr);
    }

    private void addHistory(ReturnStatus from, ReturnStatus to, String by, String note) {
        this.histories.add(ReturnHistory.of(UUID.randomUUID().toString(), from, to, by, note));
    }

    public static boolean canCreateReturn(LocalDateTime deliveredAt, int returnWindowDays) {
        return deliveredAt.plusDays(returnWindowDays).isAfter(LocalDateTime.now());
    }
}
