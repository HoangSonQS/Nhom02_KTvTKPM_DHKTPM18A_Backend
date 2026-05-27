package iuh.fit.se.modules.ai.adapter.inbound.web;

import iuh.fit.se.modules.ai.application.port.in.AiVisionUseCase;
import iuh.fit.se.modules.ai.domain.OcrResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ai/book-recognize")
@RequiredArgsConstructor
public class AiVisionController {

    private final AiVisionUseCase visionUseCase;

    @PostMapping
    public ResponseEntity<OcrResult> recognize(@RequestParam("file") MultipartFile file) throws java.io.IOException {
        OcrResult result = visionUseCase.recognizeBook(file.getBytes());
        return ResponseEntity.ok(result);
    }
}
