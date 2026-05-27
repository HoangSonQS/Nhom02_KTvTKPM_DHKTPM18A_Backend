package iuh.fit.se.modules.ai.application.service;

import iuh.fit.se.modules.ai.application.port.in.AiVisionUseCase;
import iuh.fit.se.modules.ai.application.port.out.VisionModelPort;
import iuh.fit.se.modules.ai.domain.OcrResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiVisionService implements AiVisionUseCase {

    private final VisionModelPort visionPort;

    @Override
    public OcrResult recognizeBook(byte[] file) {
        // 1. OCR Extract data
        OcrResult result = visionPort.extractBookData(file);

        if (result.isDetected()) {
            log.info("Book recognized: {} by {}", result.getTitle(), result.getAuthor());
            // Optional: We could do a search right here and attach results to OcrResult
            // String query = String.format("%s %s", result.getTitle(), result.getAuthor());
            // List<Long> bookIds = searchUseCase.searchSemantic(query, 5);
            // result.setSuggestedBookIds(bookIds);
        }

        return result;
    }
}
