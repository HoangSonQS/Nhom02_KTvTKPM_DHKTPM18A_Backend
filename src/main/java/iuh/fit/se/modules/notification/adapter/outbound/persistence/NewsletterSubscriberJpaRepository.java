package iuh.fit.se.modules.notification.adapter.outbound.persistence;

import iuh.fit.se.modules.notification.domain.NewsletterSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NewsletterSubscriberJpaRepository extends JpaRepository<NewsletterSubscriber, Long> {

    Optional<NewsletterSubscriber> findByEmail(String email);

    List<NewsletterSubscriber> findByActiveTrueOrderByCreatedAtAsc();
}
