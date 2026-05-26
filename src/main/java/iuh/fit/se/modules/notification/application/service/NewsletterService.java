package iuh.fit.se.modules.notification.application.service;

import iuh.fit.se.modules.notification.application.port.in.NewsletterUseCase;
import iuh.fit.se.modules.notification.application.port.out.NewsletterSubscriberPersistencePort;
import iuh.fit.se.modules.notification.domain.NewsletterSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NewsletterService implements NewsletterUseCase {

    private final NewsletterSubscriberPersistencePort persistencePort;

    @Override
    @Transactional
    public NewsletterSubscriptionResponse subscribe(String email) {
        String normalizedEmail = normalizeEmail(email);
        NewsletterSubscriber subscriber = persistencePort.findByEmail(normalizedEmail)
                .map(existing -> {
                    existing.reactivate();
                    return existing;
                })
                .orElseGet(() -> NewsletterSubscriber.subscribe(normalizedEmail));

        NewsletterSubscriber saved = persistencePort.save(subscriber);
        return new NewsletterSubscriptionResponse(saved.getEmail(), saved.isActive());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
