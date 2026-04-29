package iuh.fit.se.shared.event.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookCreatedEvent implements Serializable {
    @Builder.Default
    private final UUID eventId = UUID.randomUUID();
    @Builder.Default
    private final int eventVersion = 1;

    private Long bookId;
    private int initialQuantity;
}
