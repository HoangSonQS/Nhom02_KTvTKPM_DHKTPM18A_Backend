package iuh.fit.se.modules.order.application.port.in;

import iuh.fit.se.modules.order.domain.FulfillmentStatus;
import lombok.Builder;
import lombok.Data;

public interface OrderInternalUseCase {

    OrderResponse checkout(Long userId, CheckoutCommand command);

    OrderResponse getOrderById(Long orderId);

    java.util.List<OrderResponse> getMyOrders(Long userId);

    OrderResponse getMyOrderById(Long orderId, Long userId);

    OrderResponse cancelMyPendingOrder(Long orderId, Long userId, String reason);

    OrderResponse confirmMyOrderReceived(Long orderId, Long userId);

    OrderResponse confirmMyPendingOrderAsCod(Long orderId, Long userId);

    void markOrderAsPaid(Long orderId);

    // Admin/Staff methods
    java.util.List<OrderResponse> getAllOrders();

    java.util.List<AdminOrderResponse> searchAdminOrders(AdminOrderSearchCriteria criteria);

    AdminOrderResponse getAdminOrderById(Long orderId);

    java.util.List<TopSellingBookResponse> getTopSellingBooks(int limit);

    java.util.List<BookSalesResponse> getBookSales(java.time.LocalDate fromDate, java.time.LocalDate toDate);

    /**
     * Cập nhật FulfillmentStatus đơn hàng theo luồng admin operational transition.
     */
    OrderResponse updateOrderStatus(Long orderId, UpdateFulfillmentStatusCommand command);

    /**
     * Force-cancel đơn hàng với lý do. Dùng cho admin override và payment timeout.
     * Không cần đi qua transition guard isValidAdminTransition.
     */
    OrderResponse cancelOrder(Long orderId, String reason);

    @Data
    @Builder
    class OrderResponse {
        private Long orderId;
        private Long userId;
        private java.math.BigDecimal totalAmount;
        private java.math.BigDecimal discountAmount;
        private java.math.BigDecimal finalAmount;

        /**
         * fulfillmentStatus — source of truth cho trạng thái xử lý đơn hàng.
         * Các giá trị: PENDING, CONFIRMED, PROCESSING, DELIVERING, DELIVERED, CANCELLED.
         */
        private String fulfillmentStatus;

        private String sagaStatus;
        private String requestId;
        private java.time.LocalDateTime updatedAt;
        private java.util.List<OrderItemResponse> items;
    }

    @Data
    @Builder
    class AdminOrderResponse {
        private Long orderId;
        private Long userId;
        private String customerName;
        private String customerEmail;
        private String customerPhone;
        private String shippingAddress;
        private java.math.BigDecimal totalAmount;
        private java.math.BigDecimal discountAmount;
        private java.math.BigDecimal finalAmount;
        private String fulfillmentStatus;
        private String sagaStatus;
        private String requestId;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
        private java.util.List<OrderItemResponse> items;
    }

    @Data
    @Builder
    class OrderItemResponse {
        private Long bookId;
        private String title;
        private int quantity;
        private java.math.BigDecimal priceAtPurchase;
    }

    @Data
    @Builder
    class AdminOrderSearchCriteria {
        private FulfillmentStatus status;
        private String customerKeyword;
    }

    record TopSellingBookResponse(
            Long bookId,
            String title,
            long quantitySold,
            java.math.BigDecimal revenue
    ) {}

    record BookSalesResponse(
            Long bookId,
            String title,
            long quantitySold,
            java.math.BigDecimal revenue
    ) {}

    @Data
    @Builder
    class CheckoutCommand {
        private String requestId;
        private String shippingAddress;
        private String customerPhone;
        private String couponCode;
        private String paymentMethod;
        private java.util.List<Long> selectedBookIds;
        private java.util.Map<Long, Integer> selectedBookQuantities;
    }

    @Data
    @Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class UpdateFulfillmentStatusCommand {
        private FulfillmentStatus newStatus;
        private String reason;
    }
}
