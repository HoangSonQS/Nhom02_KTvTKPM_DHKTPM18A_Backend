package iuh.fit.se.modules.ai.domain;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
public class OcrResult {
    private String title;
    private String author;
    private String rawText;
    private boolean detected;
}
