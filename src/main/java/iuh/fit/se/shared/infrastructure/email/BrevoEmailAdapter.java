package iuh.fit.se.shared.infrastructure.email;

import iuh.fit.se.shared.application.port.out.EmailPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * BrevoEmailAdapter — Triển khai EmailPort sử dụng API của Brevo (Sendinblue).
 * Thiết kế bền bỉ với Timeout, Resilience và Fallback (Staff+ Standard).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BrevoEmailAdapter implements EmailPort {

    private final RestTemplate restTemplate;
    private final TemplateEngine templateEngine;

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name:SEBook Store}")
    private String senderName;

    @Value("${brevo.api.url:https://api.brevo.com/v3/smtp/email}")
    private String apiUrl;

    @Override
    public void sendTemplateEmail(String toEmail, String subject, String templateName, Map<String, Object> variables) throws Exception {
        String htmlContent;
        try {
            Context context = new Context();
            context.setVariables(variables);
            htmlContent = templateEngine.process(templateName, context);
        } catch (Exception e) {
            log.error("Failed to render Thymeleaf template: {}. Falling back to simple content.", templateName);
            // Fallback: Gửi email dạng văn bản thuần dựa trên biến quan trọng nếu render HTML lỗi
            htmlContent = "Thông báo từ SEBook: " + variables.getOrDefault("itemsSummary", "Đơn hàng của bạn");
        }

        executeSend(toEmail, subject, htmlContent);
    }

    @Override
    public void sendSimpleEmail(String toEmail, String subject, String content) throws Exception {
        executeSend(toEmail, subject, content);
    }

    private void executeSend(String toEmail, String subject, String htmlContent) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("sender", Map.of("email", senderEmail, "name", senderName));
        body.put("to", Collections.singletonList(Map.of("email", toEmail)));
        body.put("subject", subject);
        body.put("htmlContent", htmlContent);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Brevo API error: " + response.getStatusCode() + " - " + response.getBody());
            }
            log.info("✅ Email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Failed to send email to {}: {}", toEmail, e.getMessage());
            throw e; // Để Listener có thể nhận diện và thực hiện Retry
        }
    }

    /**
     * Kiểm tra sức khỏe dịch vụ Brevo (Lightweight check).
     * Gọi endpoint lấy thông tin tài khoản để xác nhận API Key còn hạn và service đang sống.
     */
    public boolean isHealthy() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("api-key", apiKey);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            // Endpoint lấy thông tin account (GET v3/account)
            String accountUrl = apiUrl.replace("/smtp/email", "/account");
            
            ResponseEntity<String> response = restTemplate.exchange(
                    accountUrl, 
                    org.springframework.http.HttpMethod.GET, 
                    request, 
                    String.class
            );
            
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("⚠️ Brevo Health Check failed: {}", e.getMessage());
            return false;
        }
    }
}
