package iuh.fit.se.modules.notification.application.port.in;

import java.util.List;

/**
 * NotificationAdminPort — Cung cấp các thao tác quản trị cho tệp Log thông báo.
 * Port này được thiết kế để module Admin có thể sử dụng mà không cần truy cập trực tiếp DB của Notification.
 */
public interface NotificationAdminPort {
    
    /**
     * Lấy danh sách các thông báo bị lỗi vĩnh viễn (Dead Letter).
     */
    List<NotificationLogResponse> getFailedNotifications();

    /**
     * Yêu cầu thực hiện lại (Retry) một thông báo bị lỗi.
     * @param logId ID của bản ghi log cần retry.
     */
    void retryNotification(Long logId);
}
