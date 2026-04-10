package iuh.fit.se.modules.ai.adapter.inbound.web;

import iuh.fit.se.modules.ai.application.port.in.EmbeddingSyncUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ai/sync-all")
@RequiredArgsConstructor
public class AdminAiSyncController {

    private final EmbeddingSyncUseCase syncUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> syncAll() {
        // Run syncAllBooks in background if needed, but for now we call it directly
        // and EmbeddingSyncService.syncBook is @Async
        syncUseCase.syncAllBooks();
        return ResponseEntity.ok("Bulk synchronization triggered successfully.");
    }
}
