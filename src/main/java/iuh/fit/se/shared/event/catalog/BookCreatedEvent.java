package iuh.fit.se.shared.event.catalog;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BookCreatedEvent implements Serializable {
    private Long bookId;
}
