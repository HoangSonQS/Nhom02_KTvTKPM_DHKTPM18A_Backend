package iuh.fit.se.modules.notification.application.service;

import iuh.fit.se.modules.notification.application.port.in.NewsletterUseCase;
import iuh.fit.se.modules.notification.application.port.out.NewsletterSubscriberPersistencePort;
import iuh.fit.se.modules.notification.domain.NewsletterSubscriber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsletterServiceTest {

    @Mock
    private NewsletterSubscriberPersistencePort persistencePort;

    @InjectMocks
    private NewsletterService service;

    @Test
    void givenNewEmail_whenSubscribe_thenNormalizeAndSaveActiveSubscriber() {
        when(persistencePort.findByEmail("reader@example.com")).thenReturn(Optional.empty());
        when(persistencePort.save(argThat(subscriber ->
                "reader@example.com".equals(subscriber.getEmail()) && subscriber.isActive()
        ))).thenAnswer(invocation -> invocation.getArgument(0));

        NewsletterUseCase.NewsletterSubscriptionResponse result = service.subscribe(" Reader@Example.COM ");

        assertThat(result.email()).isEqualTo("reader@example.com");
        assertThat(result.active()).isTrue();
        verify(persistencePort).findByEmail("reader@example.com");
    }

    @Test
    void givenExistingEmail_whenSubscribe_thenReactivateSubscriber() {
        NewsletterSubscriber existing = NewsletterSubscriber.subscribe("reader@example.com");
        when(persistencePort.findByEmail("reader@example.com")).thenReturn(Optional.of(existing));
        when(persistencePort.save(existing)).thenReturn(existing);

        NewsletterUseCase.NewsletterSubscriptionResponse result = service.subscribe("reader@example.com");

        assertThat(result.email()).isEqualTo("reader@example.com");
        assertThat(result.active()).isTrue();
        verify(persistencePort).save(existing);
    }
}
