package iuh.fit.se.modules.ai;

import iuh.fit.se.modules.ai.application.port.in.EmbeddingSyncUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
public class ManualSyncTest {

    @Autowired
    private EmbeddingSyncUseCase syncUseCase;

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
