package iuh.fit.se.modules.ai.application.port.out;

import iuh.fit.se.modules.ai.domain.BookVectorMetadata;
import java.util.List;

public interface VectorStorePort {
    void saveBookVector(BookVectorMetadata metadata);
    List<Long> findSimilarBooks(String query, int topK);
}
