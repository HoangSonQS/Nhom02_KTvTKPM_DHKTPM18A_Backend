package iuh.fit.se.modules.ai.adapter.outbound.persistence;

import iuh.fit.se.modules.ai.application.port.out.VectorStorePort;
import iuh.fit.se.modules.ai.domain.BookVectorMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PgVectorStoreAdapter implements VectorStorePort {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void saveBookVector(BookVectorMetadata metadata) {
        // Sử dụng UUID định danh từ bookId để đảm bảo tính nhất quán và tương thích với cột UUID của DB
        String docId = java.util.UUID.nameUUIDFromBytes(String.valueOf(metadata.getBookId()).getBytes()).toString();

        Document doc = new Document(
                docId,
                metadata.toContentString(),
                Map.of(
                        "bookId", metadata.getBookId(),
                        "title", metadata.getTitle(),
                        "author", metadata.getAuthor(),
                        "category", metadata.getCategory(),
                        "contentHash", metadata.getContentHash(),
                        "embeddingVersion", metadata.getEmbeddingVersion()));

        vectorStore.add(List.of(doc));

        // Cập nhật các cột chuyên biệt (content_hash, embedding_version) sau khi Spring
        // AI đã lưu vào table
        // Lưu ý: bảng mặc định là ai_book_vectors
        try {
            jdbcTemplate.update(
                    "UPDATE ai_book_vectors SET content_hash = ?, embedding_version = ? WHERE (metadata->>'bookId')::bigint = ?",
                    metadata.getContentHash(),
                    metadata.getEmbeddingVersion(),
                    metadata.getBookId());
        } catch (Exception e) {
            log.error("Failed to update extra columns for bookId: {}", metadata.getBookId(), e);
        }
    }

    @Override
    public List<Long> findSimilarBooks(String query, int topK) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .build());

        return results.stream()
                .map(doc -> Long.valueOf(doc.getMetadata().get("bookId").toString()))
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public void deleteBookVector(Long bookId) {
        // Chuyển bookId sang UUID string tương ứng để xóa chính xác document
        String docId = java.util.UUID.nameUUIDFromBytes(String.valueOf(bookId).getBytes()).toString();
        vectorStore.delete(List.of(docId));
    }

    @Override
    public Optional<BookVectorMetadata> getExistingMetadata(Long bookId) {
        String sql = "SELECT content_hash, embedding_version FROM ai_book_vectors WHERE (metadata->>'bookId')::bigint = ? LIMIT 1";
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> BookVectorMetadata.builder()
                    .bookId(bookId)
                    .contentHash(rs.getString("content_hash"))
                    .embeddingVersion(rs.getInt("embedding_version"))
                    .build(), bookId).stream().findFirst();
        } catch (Exception e) {
            log.error("Error fetching existing vector metadata for bookId: {}", bookId, e);
            return Optional.empty();
        }
    }
}
