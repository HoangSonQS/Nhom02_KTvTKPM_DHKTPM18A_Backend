package iuh.fit.se.modules.ai.application.port.in;

import iuh.fit.se.modules.catalog.domain.Book;
import java.util.List;

public interface AiSearchUseCase {
    List<Long> searchSemantic(String query, int topK);
}
