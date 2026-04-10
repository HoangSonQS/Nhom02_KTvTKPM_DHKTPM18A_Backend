package iuh.fit.se.shared.event.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Event phát ra khi thông tin sách được cập nhật.
 * Thiết kế Lean Payload & Versioning để tối ưu cho AI Consumer.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookUpdatedEvent implements Serializable {
    @Builder.Default
    private final UUID eventId = UUID.randomUUID();
    @Builder.Default
    private final int eventVersion = 1;
    
    private Long bookId;
    private String title;
    private String author;
    private String description;
}
