package iuh.fit.se.modules.returns.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ret_return_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ReturnItem {

    @Id
    private String id;

    @Column(name = "return_request_id", nullable = false)
    private String returnRequestId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "refund_price", nullable = false)
    private BigDecimal refundPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false)
    private ItemCondition condition;

    public static ReturnItem create(String id, Long bookId, int quantity, BigDecimal refundPrice, ItemCondition condition) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
        }
        return ReturnItem.builder()
                .id(id)
                .bookId(bookId)
                .quantity(quantity)
                .refundPrice(refundPrice)
                .condition(condition)
                .build();
    }

    public void setReturnRequestId(String returnRequestId) {
        this.returnRequestId = returnRequestId;
    }
}
