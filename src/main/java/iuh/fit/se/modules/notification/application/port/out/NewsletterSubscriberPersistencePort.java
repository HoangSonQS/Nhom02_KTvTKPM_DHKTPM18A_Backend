package iuh.fit.se.modules.notification.application.port.out;

import iuh.fit.se.modules.notification.domain.NewsletterSubscriber;

import java.util.List;
import java.util.Optional;

public interface NewsletterSubscriberPersistencePort {

    Optional<NewsletterSubscriber> findByEmail(String email);

    List<NewsletterSubscriber> findActiveSubscribers();

    NewsletterSubscriber save(NewsletterSubscriber subscriber);
}
