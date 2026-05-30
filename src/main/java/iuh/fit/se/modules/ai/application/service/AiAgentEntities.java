package iuh.fit.se.modules.ai.application.service;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiAgentEntities(
        @JsonAlias({"bookTitle", "bookName"})
        String bookName,
        Long bookId,
        Integer quantity,
        String couponCode,
        String paymentMethod,
        String shippingAddress,
        String customerPhone,
        Long orderId,
        String category,
        String author
) {
    public AiAgentEntities() {
        this(null, null, null, null, null, null, null, null, null, null);
    }

    public int normalizedQuantity() {
        if (quantity == null) {
            return 1;
        }
        return quantity;
    }
}
