package iuh.fit.se.modules.ai.adapter.outbound.persistence;

import iuh.fit.se.modules.ai.domain.ChatSession;
import iuh.fit.se.modules.ai.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

interface JpaChatSessionRepository extends JpaRepository<ChatSession, String> {
}

interface JpaChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
}
