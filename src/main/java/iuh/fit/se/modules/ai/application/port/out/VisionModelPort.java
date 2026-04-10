package iuh.fit.se.modules.ai.application.port.out;

import iuh.fit.se.modules.ai.domain.OcrResult;
import org.springframework.web.multipart.MultipartFile;

public interface VisionModelPort {
    OcrResult extractBookData(MultipartFile file);
}
