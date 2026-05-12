package iuh.fit.se.modules.ai;

import iuh.fit.se.modules.ai.application.port.in.EmbeddingSyncUseCase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Manual test for triggering sync, requires full environment setup")
public class ManualSyncTest {

    @Autowired
    private EmbeddingSyncUseCase syncUseCase;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    void triggerSync() {
        System.out.println(">>> TRIGGERING SYNC ALL BOOKS <<<");
        syncUseCase.syncAllBooks();
        // Wait for @Async tasks to complete
        try {
            Thread.sleep(30000); 
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(">>> SYNC TRIGGERED <<<");
    }
}
