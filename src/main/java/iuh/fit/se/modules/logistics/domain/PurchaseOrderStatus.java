package iuh.fit.se.modules.logistics.domain;

public enum PurchaseOrderStatus {
    DRAFT,      // Bản nháp, có thể sửa
    SUBMITTED,  // Đã gửi duyệt
    APPROVED,   // Đã được Admin phê duyệt, chờ nhập hàng
    RECEIVED,   // Đã nhập hàng thành công vào kho
    CANCELLED   // Đã hủy
}
