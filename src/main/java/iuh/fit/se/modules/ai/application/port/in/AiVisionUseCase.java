package iuh.fit.se.modules.ai.application.port.in;

import iuh.fit.se.modules.ai.domain.OcrResult;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface AiVisionUseCase {
    OcrResult recognizeBook(MultipartFile file);
}
