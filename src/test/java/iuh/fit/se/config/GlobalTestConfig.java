package iuh.fit.se.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import iuh.fit.se.shared.application.port.out.EmailPort;
import org.mockito.Mockito;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

@TestConfiguration
public class GlobalTestConfig {

    @Bean
    @Primary
    public EmailPort emailPort() {
        return Mockito.mock(EmailPort.class);
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        return Mockito.mock(StringRedisTemplate.class);
    }

    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        return Mockito.mock(EmbeddingModel.class);
    }

    @Bean
    @Primary
    public ChatModel chatModel() {
        return Mockito.mock(ChatModel.class);
    }
}
