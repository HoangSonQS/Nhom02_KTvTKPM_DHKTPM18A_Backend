package iuh.fit.se.modules.ai.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_processed_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiProcessedEventJpaEntity {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "PROCESSING";

    @Column(name = "processed_at")
    @Builder.Default
    private LocalDateTime processedAt = LocalDateTime.now();
}
