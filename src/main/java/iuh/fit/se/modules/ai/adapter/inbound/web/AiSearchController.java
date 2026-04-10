package iuh.fit.se.modules.ai.adapter.inbound.web;

import iuh.fit.se.modules.ai.application.port.in.AiSearchUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/search")
@RequiredArgsConstructor
public class AiSearchController {

    private final AiSearchUseCase searchUseCase;

    @GetMapping
    public ResponseEntity<List<Long>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int topK) {
        List<Long> bookIds = searchUseCase.searchSemantic(q, topK);
        return ResponseEntity.ok(bookIds);
    }
}
