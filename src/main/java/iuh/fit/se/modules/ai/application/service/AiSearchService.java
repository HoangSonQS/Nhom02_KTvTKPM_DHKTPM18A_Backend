package iuh.fit.se.modules.ai.application.service;

import iuh.fit.se.modules.ai.application.port.in.AiSearchUseCase;
import iuh.fit.se.modules.ai.application.port.out.VectorStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiSearchService implements AiSearchUseCase {

    private final VectorStorePort vectorStorePort;

    @Override
    public List<Long> searchSemantic(String query, int topK) {
        return vectorStorePort.findSimilarBooks(query, topK);
    }
}
