package iuh.fit.se.modules.returns.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ret_return_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ReturnHistory {

    @Id
    private String id;

    @Column(name = "return_request_id", nullable = false)
    private String returnRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private ReturnStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private ReturnStatus toStatus;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @CreationTimestamp
    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    public static ReturnHistory of(String id, ReturnStatus from, ReturnStatus to, String by, String note) {
        return ReturnHistory.builder()
                .id(id)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(by)
                .note(note)
                .build();
    }

    public void setReturnRequestId(String returnRequestId) {
        this.returnRequestId = returnRequestId;
    }
}
