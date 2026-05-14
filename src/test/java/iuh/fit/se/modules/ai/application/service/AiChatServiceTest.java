package iuh.fit.se.modules.ai.application.service;

import iuh.fit.se.modules.ai.application.port.out.ChatHistoryPersistencePort;
import iuh.fit.se.modules.ai.application.port.out.LlmPort;
import iuh.fit.se.modules.ai.application.port.out.VectorStorePort;
import iuh.fit.se.modules.ai.domain.ChatMessage;
import iuh.fit.se.modules.ai.domain.ChatRole;
import iuh.fit.se.modules.catalog.application.port.in.BookDTO;
import iuh.fit.se.modules.catalog.application.port.in.BookUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock
    private LlmPort llmPort;

    @Mock
    private ChatHistoryPersistencePort historyPort;

    @Mock
    private VectorStorePort vectorStorePort;

    @Mock
    private BookUseCase bookUseCase;

    private AiChatService aiChatService;

    @BeforeEach
    void setUp() {
        aiChatService = new AiChatService(llmPort, historyPort, vectorStorePort, bookUseCase);
    }

    @Test
    void givenLlmFailure_whenChat_thenReturnFallbackAndSaveAssistantMessage() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("goi y sach van hoc", 5)).thenReturn(List.of());
        when(llmPort.chat(eq("goi y sach van hoc"), any())).thenThrow(new RuntimeException("Gemini unavailable"));

        String response = aiChatService.chat("session-1", 2L, "goi y sach van hoc");

        assertEquals(AiChatService.FALLBACK_RESPONSE, response);

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(historyPort, atLeastOnce()).saveSession(any());
        verify(historyPort, times(2)).saveMessage(messageCaptor.capture());

        List<ChatMessage> savedMessages = messageCaptor.getAllValues();
        assertEquals(ChatRole.USER, savedMessages.get(0).getRole());
        assertEquals(ChatRole.ASSISTANT, savedMessages.get(1).getRole());
        assertEquals(AiChatService.FALLBACK_RESPONSE, savedMessages.get(1).getContent());
    }

    @Test
    void givenUserMessageWithoutCatalogContext_whenChat_thenSendCurrentMessageAsPromptOnly() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("goi y sach van hoc", 5)).thenReturn(List.of());
        when(llmPort.chat(eq("goi y sach van hoc"), any())).thenReturn("Ban co the doc Tat den.");

        aiChatService.chat("session-1", 2L, "goi y sach van hoc");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessage>> historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(llmPort).chat(eq("goi y sach van hoc"), historyCaptor.capture());

        List<ChatMessage> history = historyCaptor.getValue();
        long userMessageCount = history.stream()
                .filter(message -> message.getRole() == ChatRole.USER)
                .filter(message -> "goi y sach van hoc".equals(message.getContent()))
                .count();

        assertEquals(0, userMessageCount);
        assertTrue(history.stream().noneMatch(message -> message.getRole() == ChatRole.ASSISTANT));
    }

    @Test
    void givenSemanticMatches_whenChat_thenSendBookContextToLlm() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("goi y sach van hoc", 5)).thenReturn(List.of(10L, 11L));
        when(bookUseCase.getBook(10L)).thenReturn(BookDTO.builder()
                .id(10L)
                .title("Tat den")
                .author("Ngo Tat To")
                .description("Tieu thuyet hien thuc ve doi song nong dan Viet Nam.")
                .quantity(7)
                .isActive(true)
                .build());
        when(bookUseCase.getBook(11L)).thenReturn(BookDTO.builder()
                .id(11L)
                .title("So do")
                .author("Vu Trong Phung")
                .description("Tac pham trao phung kinh dien.")
                .quantity(0)
                .isActive(true)
                .build());
        when(llmPort.chat(any(), any())).thenReturn("Ban co the doc Tat den.");

        aiChatService.chat("session-1", 2L, "goi y sach van hoc");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmPort).chat(promptCaptor.capture(), any());

        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("KHO SÁCH SEBook"));
        assertTrue(prompt.contains("Tat den"));
        assertTrue(prompt.contains("Ngo Tat To"));
        assertTrue(prompt.contains("Còn hàng: 7"));
        assertTrue(prompt.contains("So do"));
        assertTrue(prompt.contains("Hết hàng"));
    }

    @Test
    void givenSemanticMatchesAndLlmFailure_whenChat_thenReturnCatalogBasedFallback() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("goi y sach van hoc", 5)).thenReturn(List.of(10L));
        when(bookUseCase.getBook(10L)).thenReturn(BookDTO.builder()
                .id(10L)
                .title("Tat den")
                .author("Ngo Tat To")
                .description("Tieu thuyet hien thuc ve doi song nong dan Viet Nam.")
                .quantity(7)
                .isActive(true)
                .build());
        when(llmPort.chat(any(), any())).thenThrow(new RuntimeException("Quota exceeded"));

        String response = aiChatService.chat("session-1", 2L, "goi y sach van hoc");

        assertTrue(response.contains("Tat den"));
        assertTrue(response.contains("Ngo Tat To"));
        assertTrue(response.contains("Còn hàng: 7"));
    }

    @Test
    void givenRequestedBookCount_whenLlmFails_thenLimitCatalogFallbackToRequestedCount() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("goi y 2 quyen sach van hoc", 5)).thenReturn(List.of(10L, 11L, 12L));
        when(bookUseCase.getBook(10L)).thenReturn(BookDTO.builder()
                .id(10L)
                .title("Tat den")
                .author("Ngo Tat To")
                .quantity(7)
                .isActive(true)
                .build());
        when(bookUseCase.getBook(11L)).thenReturn(BookDTO.builder()
                .id(11L)
                .title("So do")
                .author("Vu Trong Phung")
                .quantity(5)
                .isActive(true)
                .build());
        when(bookUseCase.getBook(12L)).thenReturn(BookDTO.builder()
                .id(12L)
                .title("Lao Hac")
                .author("Nam Cao")
                .quantity(3)
                .isActive(true)
                .build());
        when(llmPort.chat(any(), any())).thenThrow(new RuntimeException("Quota exceeded"));

        String response = aiChatService.chat("session-1", 2L, "goi y 2 quyen sach van hoc");

        assertTrue(response.contains("Tat den"));
        assertTrue(response.contains("So do"));
        assertTrue(response.contains("Vu Trong Phung"));
        assertTrue(!response.contains("Lao Hac"));
    }
}
