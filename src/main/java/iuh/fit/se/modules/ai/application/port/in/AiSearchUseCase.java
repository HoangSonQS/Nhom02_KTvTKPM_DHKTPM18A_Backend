package iuh.fit.se.modules.ai.application.port.in;

import java.util.List;

public interface AiSearchUseCase {
    List<Long> searchSemantic(String query, int topK);
}
