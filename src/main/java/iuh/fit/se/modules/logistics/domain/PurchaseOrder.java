package iuh.fit.se.modules.logistics.domain;

import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "log_purchase_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseOrderStatus status;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "received_by")
    private String receivedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Version
    private Long version;

    private String note;

    @Builder.Default
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    public static PurchaseOrder create(Supplier supplier, String createdBy, String note, List<PurchaseOrderItem> items) {
        PurchaseOrder po = PurchaseOrder.builder()
                .supplier(supplier)
                .status(PurchaseOrderStatus.DRAFT)
                .createdBy(createdBy)
                .note(note)
                .items(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .build();

        if (items != null) {
            items.forEach(po::addItem);
        }
        return po;
    }

    public void addItem(PurchaseOrderItem item) {
        item.setPurchaseOrder(this);
        this.items.add(item);
        calculateTotalAmount();
    }

    private void calculateTotalAmount() {
        this.totalAmount = items.stream()
                .map(item -> item.getPriceAtOrder().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // --- State Management ---

    public void submit(String userRole) {
        validateRole(userRole, "STAFF_WAREHOUSE", "ADMIN");
        if (this.status != PurchaseOrderStatus.DRAFT) {
            throw new AppException(ErrorCode.LOG_INVALID_STATE_TRANSITION, "Chỉ có thể submit từ trạng thái DRAFT");
        }
        this.status = PurchaseOrderStatus.SUBMITTED;
    }

    public void returnToDraft(String userRole, String reason) {
        validateRole(userRole, "ADMIN");
        if (this.status != PurchaseOrderStatus.SUBMITTED) {
            throw new AppException(ErrorCode.LOG_INVALID_STATE_TRANSITION, "Chỉ có thể trả về DRAFT từ SUBMITTED");
        }
        this.status = PurchaseOrderStatus.DRAFT;
        this.note = (this.note == null ? "" : this.note + " | ") + "Admin Return: " + reason;
    }

    public void approve(String userRole, String adminName) {
        validateRole(userRole, "ADMIN");
        if (this.status != PurchaseOrderStatus.SUBMITTED) {
            throw new AppException(ErrorCode.LOG_INVALID_STATE_TRANSITION, "Chỉ có thể duyệt từ trạng thái SUBMITTED");
        }
        this.status = PurchaseOrderStatus.APPROVED;
        this.approvedBy = adminName;
        this.approvedAt = LocalDateTime.now();
    }

    public void receive(String userRole, String receiverName) {
        validateRole(userRole, "STAFF_WAREHOUSE", "ADMIN");
        if (this.status != PurchaseOrderStatus.APPROVED) {
            throw new AppException(ErrorCode.LOG_INVALID_STATE_TRANSITION, "Chỉ có thể nhập kho sau khi đã APPROVED");
        }
        this.status = PurchaseOrderStatus.RECEIVED;
        this.receivedBy = receiverName;
        this.receivedAt = LocalDateTime.now();
    }

    public void cancel(String userRole, String userName, String reason) {
        if (this.status == PurchaseOrderStatus.APPROVED) {
            validateRole(userRole, "ADMIN");
        } else if (this.status == PurchaseOrderStatus.DRAFT || this.status == PurchaseOrderStatus.SUBMITTED) {
            validateRole(userRole, "STAFF_WAREHOUSE", "ADMIN");
        } else {
            throw new AppException(ErrorCode.LOG_INVALID_STATE_TRANSITION, "Trạng thái này không thể hủy");
        }

        this.status = PurchaseOrderStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledBy = userName;
        this.cancelledAt = LocalDateTime.now();
    }

    private void validateRole(String userRole, String... allowedRoles) {
        boolean allowed = false;
        for (String role : allowedRoles) {
            if (userRole.equals("ROLE_" + role) || userRole.equals(role)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new AppException(ErrorCode.LOG_UNAUTHORIZED_STATE_TRANSITION,
                    "Bạn không có quyền thực hiện thao tác này");
        }
    }
}