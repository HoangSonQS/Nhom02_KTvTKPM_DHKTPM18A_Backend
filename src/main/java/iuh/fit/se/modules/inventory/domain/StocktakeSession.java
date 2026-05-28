package iuh.fit.se.modules.inventory.domain;

import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inv_stocktake_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StocktakeSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StocktakeStatus status;

    @Column(name = "assigned_staff_id")
    private Long assignedStaffId;

    @Column(name = "assigned_staff_email")
    private String assignedStaffEmail;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "submitted_by")
    private String submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_by")
    private String rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Builder.Default
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StocktakeItem> items = new ArrayList<>();

    public static StocktakeSession create(String name, String createdBy, Long assignedStaffId, String assignedStaffEmail, List<StocktakeItem> items) {
        if (name == null || name.isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Tên phiên kiểm kê không được trống");
        }
        if (items == null || items.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Phiên kiểm kê phải có ít nhất một sách");
        }
        StocktakeSession session = StocktakeSession.builder()
                .name(name.trim())
                .status(StocktakeStatus.IN_PROGRESS)
                .createdBy(createdBy)
                .assignedStaffId(assignedStaffId)
                .assignedStaffEmail(assignedStaffEmail)
                .items(new ArrayList<>())
                .build();
        items.forEach(session::addItem);
        return session;
    }

    private void addItem(StocktakeItem item) {
        item.attachTo(this);
        items.add(item);
    }

    public void assignStaff(Long staffId, String staffEmail) {
        ensureEditable();
        this.assignedStaffId = staffId;
        this.assignedStaffEmail = staffEmail;
    }

    public void submit(String submittedBy) {
        ensureStatus(StocktakeStatus.IN_PROGRESS);
        boolean hasMissingActualQuantity = items.stream().anyMatch(item -> item.getActualQuantity() == null);
        if (hasMissingActualQuantity) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Vui lòng nhập đủ số lượng thực tế cho tất cả sách");
        }
        this.status = StocktakeStatus.SUBMITTED;
        this.submittedBy = submittedBy;
        this.submittedAt = LocalDateTime.now();
    }

    public void approve(String approvedBy) {
        ensureStatus(StocktakeStatus.SUBMITTED);
        this.status = StocktakeStatus.APPROVED;
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject(String rejectedBy, String reason) {
        ensureStatus(StocktakeStatus.SUBMITTED);
        if (reason == null || reason.isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Vui lòng nhập lý do từ chối");
        }
        this.status = StocktakeStatus.REJECTED;
        this.rejectedBy = rejectedBy;
        this.rejectedAt = LocalDateTime.now();
        this.rejectReason = reason.trim();
    }

    public void cancel(String cancelledBy) {
        ensureEditable();
        this.status = StocktakeStatus.CANCELLED;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = LocalDateTime.now();
    }

    public boolean isAssignedTo(Long staffId) {
        return staffId != null && staffId.equals(assignedStaffId);
    }

    private void ensureEditable() {
        if (status != StocktakeStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Chỉ có thể cập nhật phiên đang kiểm kê");
        }
    }

    private void ensureStatus(StocktakeStatus expected) {
        if (status != expected) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Trạng thái phiên kiểm kê không hợp lệ");
        }
    }
}
