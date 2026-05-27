package iuh.fit.se.modules.ai.application.port.out;

import iuh.fit.se.modules.ai.domain.OcrResult;

public interface VisionModelPort {
    OcrResult extractBookData(byte[] file);
}
