package iuh.fit.se.modules.notification.application.port.in;

public interface NewsletterUseCase {

    NewsletterSubscriptionResponse subscribe(String email);

    record NewsletterSubscriptionResponse(
            String email,
            boolean active
    ) {}
}
