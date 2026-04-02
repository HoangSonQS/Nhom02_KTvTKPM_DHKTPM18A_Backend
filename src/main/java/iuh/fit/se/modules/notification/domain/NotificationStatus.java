package iuh.fit.se.modules.notification.domain;

/**
 * NotificationStatus — Trạng thái xử lý của một sự kiện thông báo.
 */
public enum NotificationStatus {
    INIT,              // Vừa nhận được sự kiện, chuẩn bị gửi
    SUCCESS,           // Đã gửi thành công xong xuôi
    FAILED,            // Gửi lỗi (có thể retry)
    FAILED_PERMANENT   // Lỗi vĩnh viễn (hết lượt retry, cần admin can thiệp)
}
