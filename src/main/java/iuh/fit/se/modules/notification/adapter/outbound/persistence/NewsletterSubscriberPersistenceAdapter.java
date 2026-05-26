package iuh.fit.se.modules.notification.adapter.outbound.persistence;

import iuh.fit.se.modules.notification.application.port.out.NewsletterSubscriberPersistencePort;
import iuh.fit.se.modules.notification.domain.NewsletterSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NewsletterSubscriberPersistenceAdapter implements NewsletterSubscriberPersistencePort {

    private final NewsletterSubscriberJpaRepository repository;

    @Override
    public Optional<NewsletterSubscriber> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Override
    public List<NewsletterSubscriber> findActiveSubscribers() {
        return repository.findByActiveTrueOrderByCreatedAtAsc();
    }

    @Override
    public NewsletterSubscriber save(NewsletterSubscriber subscriber) {
        return repository.save(subscriber);
    }
}
