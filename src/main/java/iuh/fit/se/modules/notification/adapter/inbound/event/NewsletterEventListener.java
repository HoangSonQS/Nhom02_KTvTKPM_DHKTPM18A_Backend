package iuh.fit.se.modules.notification.adapter.inbound.event;

import iuh.fit.se.modules.notification.application.port.out.NewsletterSubscriberPersistencePort;
import iuh.fit.se.modules.notification.domain.NewsletterSubscriber;
import iuh.fit.se.modules.promotion.domain.DiscountType;
import iuh.fit.se.shared.application.port.out.EmailPort;
import iuh.fit.se.shared.event.catalog.BookCreatedEvent;
import iuh.fit.se.shared.event.promotion.CouponCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.text.NumberFormat;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsletterEventListener {

    private final NewsletterSubscriberPersistencePort subscriberPersistencePort;
    private final EmailPort emailPort;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookCreated(BookCreatedEvent event) {
        String subject = "SEBook co sach moi: " + event.getTitle();
        String content = "SEBook vua them sach moi: " + event.getTitle()
                + "\nTac gia: " + fallback(event.getAuthor(), "Dang cap nhat")
                + "\nGia: " + formatCurrency(event.getPrice())
                + "\nHay ghe SEBook de kham pha ngay.";

        sendToSubscribers(subject, content);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCouponCreated(CouponCreatedEvent event) {
        String subject = "SEBook co khuyen mai moi: " + event.code();
        String content = "SEBook vua them ma khuyen mai moi: " + event.code()
                + "\nUu dai: " + discountLabel(event.discountType(), event.discountValue())
                + "\n" + fallback(event.description(), "Hay ap dung ma khi thanh toan de nhan uu dai.");

        sendToSubscribers(subject, content);
    }

    private void sendToSubscribers(String subject, String content) {
        for (NewsletterSubscriber subscriber : subscriberPersistencePort.findActiveSubscribers()) {
            try {
                emailPort.sendSimpleEmail(subscriber.getEmail(), subject, content);
            } catch (Exception ex) {
                log.warn("Failed to send newsletter email to {}: {}", subscriber.getEmail(), ex.getMessage());
            }
        }
    }

    private String discountLabel(DiscountType discountType, java.math.BigDecimal discountValue) {
        if (discountType == DiscountType.PERCENTAGE) {
            return discountValue.stripTrailingZeros().toPlainString() + "%";
        }
        return formatCurrency(discountValue);
    }

    private String formatCurrency(java.math.BigDecimal value) {
        if (value == null) return "Dang cap nhat";
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")).format(value);
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
