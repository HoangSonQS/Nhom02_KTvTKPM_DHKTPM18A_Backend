package iuh.fit.se.shared.application.port.out;

import java.util.Map;

/**
 * EmailPort — Cổng gửi email dùng chung cho toàn hệ thống.
 * Tuân thủ Hexagonal Architecture: Interface nằm ở tầng Application, Implementation ở Infrastructure.
 */
public interface EmailPort {

    /**
     * Gửi email dựa trên template.
     *
     * @param toEmail      Địa chỉ người nhận
     * @param subject      Tiêu đề email
     * @param templateName Tên file template (Thymeleaf)
     * @param variables    Biến số truyền vào template
     * @throws Exception   Nếu có lỗi xảy ra (lỗi này sẽ được catch bởi Retry mechanism ở Listener)
     */
    void sendTemplateEmail(String toEmail, String subject, String templateName, Map<String, Object> variables) throws Exception;

    /**
     * Gửi email nội dung thuần (dùng làm fallback hoặc message đơn giản).
     */
    void sendSimpleEmail(String toEmail, String subject, String content) throws Exception;
}
