package iuh.fit.se.modules.ai.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_chat_messages")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatMessage {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static ChatMessage create(String sessionId, ChatRole role, String content) {
        return ChatMessage.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .build();
    }
}
