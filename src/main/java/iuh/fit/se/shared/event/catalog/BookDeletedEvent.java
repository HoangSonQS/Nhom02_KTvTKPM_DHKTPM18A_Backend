package iuh.fit.se.shared.event.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Event phát ra khi một cuốn sách bị xóa khỏi hệ thống.
 * Giúp AI module dọn dẹp Vector Store.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookDeletedEvent implements Serializable {
    @Builder.Default
    private final UUID eventId = UUID.randomUUID();
    @Builder.Default
    private final int eventVersion = 1;

    private Long bookId;
}
