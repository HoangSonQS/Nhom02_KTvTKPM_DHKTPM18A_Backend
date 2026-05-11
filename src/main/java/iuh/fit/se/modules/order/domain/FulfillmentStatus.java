package iuh.fit.se.modules.order.domain;

/**
 * Trạng thái hoàn thành đơn hàng (Fulfillment Lifecycle).
 *
 * <p>Enum này trả lời câu hỏi: "Đơn hàng đang ở bước xử lý nào?"
 * — hoàn toàn độc lập với PaymentStatus (module payment) và ReturnStatus (module returns).
 *
 * <p>Luồng chuẩn:
 * PENDING → CONFIRMED → PROCESSING → DELIVERING → DELIVERED
 *
 * <p>Nhánh hủy (qua forceCancel hoặc isValidAdminTransition):
 * CONFIRMED / PROCESSING / DELIVERING → CANCELLED
 *
 * <p>PENDING → CANCELLED KHÔNG đi qua isValidAdminTransition().
 * Case này được xử lý qua: hệ thống tự cancel (timeout) hoặc Order#forceCancel(reason).
 */
public enum FulfillmentStatus {

    /**
     * Đơn vừa được tạo, chờ xác nhận thanh toán.
     * Trạng thái khởi tạo duy nhất — set bởi checkout saga.
     */
    PENDING,

    /**
     * Thanh toán thành công, đơn hàng đã được xác nhận.
     * Trigger bởi PaymentSuccessIntegrationEvent → markOrderAsPaid() → confirm().
     *
     * <p>Note: Tên là CONFIRMED (không phải PAID) vì obligation của Order là
     * "được xác nhận", không phải "thu tiền". COD use case: order CONFIRMED nhưng
     * PaymentStatus vẫn PENDING — hoàn toàn hợp lệ.
     */
    CONFIRMED,

    /**
     * Nhân viên đang đóng gói / chuẩn bị hàng.
     * Admin/Staff transition: CONFIRMED → PROCESSING.
     */
    PROCESSING,

    /**
     * Đang giao hàng cho khách.
     * Admin/Staff transition: PROCESSING → DELIVERING.
     */
    DELIVERING,

    /**
     * Đã giao đến tay khách (terminal positive state).
     * Admin/Staff transition: DELIVERING → DELIVERED.
     *
     * <p>Note: Đơn ở trạng thái DELIVERED có thể có ReturnRequest liên quan
     * — xem ReturnRequest.returnStatus để biết chi tiết.
     */
    DELIVERED,

    /**
     * Đã hủy (terminal negative state).
     * Có thể đến từ: hệ thống timeout, forceCancel(reason), hoặc
     * admin/staff cancel từ CONFIRMED/PROCESSING/DELIVERING.
     */
    CANCELLED
}
