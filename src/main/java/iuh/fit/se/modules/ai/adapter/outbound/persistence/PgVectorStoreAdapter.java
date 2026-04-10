package iuh.fit.se.modules.ai.adapter.outbound.persistence;

import iuh.fit.se.modules.ai.application.port.out.VectorStorePort;
import iuh.fit.se.modules.ai.domain.BookVectorMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PgVectorStoreAdapter implements VectorStorePort {

    private final VectorStore vectorStore;

    @Override
    public void saveBookVector(BookVectorMetadata metadata) {
        Document doc = new Document(
            metadata.toContentString(),
            Map.of(
                "bookId", metadata.getBookId(),
                "title", metadata.getTitle(),
                "author", metadata.getAuthor(),
                "category", metadata.getCategory()
            )
        );
        vectorStore.add(List.of(doc));
    }

    @Override
    public List<Long> findSimilarBooks(String query, int topK) {
        List<Document> results = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build()
        );

        return results.stream()
            .map(doc -> Long.valueOf(doc.getMetadata().get("bookId").toString()))
            .distinct()
            .collect(Collectors.toList());
    }
}
