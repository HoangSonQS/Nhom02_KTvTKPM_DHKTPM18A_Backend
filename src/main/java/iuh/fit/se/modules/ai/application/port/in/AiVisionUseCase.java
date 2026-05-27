package iuh.fit.se.modules.ai.application.port.in;

import iuh.fit.se.modules.ai.domain.OcrResult;

public interface AiVisionUseCase {
    OcrResult recognizeBook(byte[] file);
}
