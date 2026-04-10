package iuh.fit.se.modules.ai.domain;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
public class BookVectorMetadata {
    private Long bookId;
    private String title;
    private String description;
    private String author;
    private String category;
    private String contentHash;
    private int embeddingVersion;

    public String toContentString() {
        return String.format("Title: %s. Author: %s. Category: %s. Description: %s", 
                title, author, category, description);
    }
}
