package iuh.fit.se.modules.ai.domain;

import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_agent_pending_action")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AiAgentPendingAction {

    @Id
    private String id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiAgentIntent intent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiAgentPendingActionStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static AiAgentPendingAction create(
            String sessionId,
            Long userId,
            AiAgentIntent intent,
            String payload,
            LocalDateTime expiresAt
    ) {
        if (userId == null) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Vui lòng đăng nhập để thực hiện thao tác này");
        }
        return AiAgentPendingAction.builder()
                .id(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .userId(userId)
                .intent(intent.normalized())
                .status(AiAgentPendingActionStatus.PENDING)
                .payload(payload)
                .expiresAt(expiresAt)
                .build();
    }

    public void ensureUsableBy(Long requesterId) {
        if (!userId.equals(requesterId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (status == null) {
            status = AiAgentPendingActionStatus.PENDING;
        }
        if (expiresAt.isBefore(LocalDateTime.now())) {
            status = AiAgentPendingActionStatus.EXPIRED;
            throw new AppException(ErrorCode.INVALID_INPUT, "Thao tác xác nhận đã hết hạn");
        }
        if (status != AiAgentPendingActionStatus.PENDING || confirmedAt != null || cancelledAt != null) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Thao tác xác nhận đã được xử lý");
        }
    }

    public void confirm() {
        this.confirmedAt = LocalDateTime.now();
        this.status = AiAgentPendingActionStatus.CONFIRMED;
    }

    public void cancel(Long requesterId) {
        ensureUsableBy(requesterId);
        this.cancelledAt = LocalDateTime.now();
        this.status = AiAgentPendingActionStatus.CANCELLED;
    }

    public void markExpired() {
        this.status = AiAgentPendingActionStatus.EXPIRED;
    }

    public void markFailed() {
        this.status = AiAgentPendingActionStatus.FAILED;
    }
}
