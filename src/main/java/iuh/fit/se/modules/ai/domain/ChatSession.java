package iuh.fit.se.modules.ai.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_chat_sessions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatSession {

    @Id
    private String id;

    @Column(name = "customer_id")
    private Long customerId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "session_id")
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    public static ChatSession create(String id, Long customerId) {
        return ChatSession.builder()
                .id(id)
                .customerId(customerId)
                .messages(new ArrayList<>())
                .build();
    }

    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        this.lastActiveAt = LocalDateTime.now();
    }
}
