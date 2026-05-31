package iuh.fit.se.modules.ai.application.service;

import iuh.fit.se.modules.ai.application.port.out.CatalogBookPort;
import iuh.fit.se.modules.ai.application.port.out.CatalogBookPort.BookContext;
import iuh.fit.se.modules.ai.application.port.out.ChatHistoryPersistencePort;
import iuh.fit.se.modules.ai.application.port.out.LlmPort;
import iuh.fit.se.modules.ai.application.port.out.SalesRankingPort;
import iuh.fit.se.modules.ai.application.port.out.VectorStorePort;
import iuh.fit.se.modules.ai.domain.ChatMessage;
import iuh.fit.se.modules.ai.domain.ChatRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock
    private LlmPort llmPort;

    @Mock
    private ChatHistoryPersistencePort historyPort;

    @Mock
    private VectorStorePort vectorStorePort;

    @Mock
    private CatalogBookPort catalogBookPort;

    @Mock
    private SalesRankingPort salesRankingPort;

    private AiChatService aiChatService;

    @BeforeEach
    void setUp() {
        aiChatService = new AiChatService(llmPort, historyPort, vectorStorePort, catalogBookPort, salesRankingPort);
    }

    @Test
    void givenLlmFailure_whenChat_thenReturnFallbackAndSaveAssistantMessage() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("mot cau hoi khac", 5)).thenReturn(List.of());
        when(llmPort.chat(eq("mot cau hoi khac"), any())).thenThrow(new RuntimeException("Gemini unavailable"));

        String response = aiChatService.chat("session-1", 2L, "mot cau hoi khac");

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
        when(vectorStorePort.findSimilarBooks("toi muon tro chuyen them", 5)).thenReturn(List.of());
        when(llmPort.chat(eq("toi muon tro chuyen them"), any())).thenReturn("Ban co the doc Tat den.");

        aiChatService.chat("session-1", 2L, "toi muon tro chuyen them");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessage>> historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(llmPort).chat(eq("toi muon tro chuyen them"), historyCaptor.capture());

        List<ChatMessage> history = historyCaptor.getValue();
        long userMessageCount = history.stream()
                .filter(message -> message.getRole() == ChatRole.USER)
                .filter(message -> "toi muon tro chuyen them".equals(message.getContent()))
                .count();

        assertEquals(0, userMessageCount);
        assertTrue(history.stream().noneMatch(message -> message.getRole() == ChatRole.ASSISTANT));
    }

    @Test
    void givenGreeting_whenChat_thenReturnNaturalGreetingWithoutCallingRemoteAi() {
        when(historyPort.findById("session-greeting")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-greeting")).thenReturn(List.of());

        String response = aiChatService.chat("session-greeting", 2L, "Xin chao");

        assertTrue(response.contains("Xin ch\u00e0o"));
        assertTrue(response.contains("SEBook"));
        verify(vectorStorePort, never()).findSimilarBooks(any(), any(Integer.class));
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenHowToOrderQuestion_whenChat_thenReturnNaturalGuidanceWithoutCallingRemoteAi() {
        when(historyPort.findById("session-guide")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-guide")).thenReturn(List.of());

        String response = aiChatService.chat("session-guide", 2L, "Lam sao de dat hang tren website?");

        assertTrue(response.contains("gi\u1ecf h\u00e0ng"));
        assertTrue(response.contains("thanh to\u00e1n"));
        verify(vectorStorePort, never()).findSimilarBooks(any(), any(Integer.class));
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenBroadStudentRecommendation_whenChat_thenAskForMorePreferenceWithoutCallingRemoteAi() {
        when(historyPort.findById("session-student")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-student")).thenReturn(List.of());

        String response = aiChatService.chat("session-student", 2L, "Sach nao phu hop cho sinh vien?");

        assertTrue(response.contains("ng\u00e0nh"));
        assertTrue(response.contains("ch\u1ee7 \u0111\u1ec1"));
        verify(vectorStorePort, never()).findSimilarBooks(any(), any(Integer.class));
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenSemanticMatches_whenCatalogIntent_thenReturnCatalogResponseWithoutLlm() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("goi y sach van hoc", 5)).thenReturn(List.of(10L, 11L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Tat den")
                .author("Ngo Tat To")
                .description("Tieu thuyet hien thuc ve doi song nong dan Viet Nam.")
                .categoryNames(Set.of("Van hoc"))
                .quantity(7)
                .isActive(true)
                .build());
        when(catalogBookPort.getBook(11L)).thenReturn(BookContext.builder()
                .id(11L)
                .title("So do")
                .author("Vu Trong Phung")
                .description("Tac pham trao phung kinh dien.")
                .categoryNames(Set.of("Van hoc"))
                .quantity(0)
                .isActive(true)
                .build());

        String response = aiChatService.chat("session-1", 2L, "goi y sach van hoc");

        assertTrue(response.contains("Tat den"));
        assertTrue(response.contains("Ngo Tat To"));
        assertTrue(response.contains("Còn hàng: 7"));
        assertTrue(response.contains("/books/10"));
        assertTrue(response.contains("So do"));
        assertTrue(response.contains("Hết hàng"));
        assertTrue(response.contains("/books/11"));
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenRequestedLiteratureBooks_whenVectorHasTooFewMatches_thenSupplementFromCatalog() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("goi y 3 sach van hoc", 5)).thenReturn(List.of(10L, 11L, 12L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Tat den")
                .author("Ngo Tat To")
                .categoryNames(Set.of("Van hoc"))
                .quantity(7)
                .isActive(true)
                .build());
        when(catalogBookPort.getBook(11L)).thenReturn(BookContext.builder()
                .id(11L)
                .title("Truyen ke truoc gio ngu")
                .author("SEBook Kids")
                .categoryNames(Set.of("Thieu nhi"))
                .quantity(60)
                .isActive(true)
                .build());
        when(catalogBookPort.getBook(12L)).thenReturn(BookContext.builder()
                .id(12L)
                .title("So do")
                .author("Vu Trong Phung")
                .categoryNames(Set.of("Van hoc"))
                .quantity(5)
                .isActive(true)
                .build());
        when(catalogBookPort.searchBooksByCategoryName("van hoc")).thenReturn(List.of(
                BookContext.builder()
                        .id(10L)
                        .title("Tat den")
                        .author("Ngo Tat To")
                        .categoryNames(Set.of("Van hoc"))
                        .quantity(7)
                        .isActive(true)
                        .build(),
                BookContext.builder()
                        .id(12L)
                        .title("So do")
                        .author("Vu Trong Phung")
                        .categoryNames(Set.of("Van hoc"))
                        .quantity(5)
                        .isActive(true)
                        .build(),
                BookContext.builder()
                        .id(13L)
                        .title("Frankenstein")
                        .author("Mary Shelley")
                        .categoryNames(Set.of("Van hoc"))
                        .quantity(7)
                        .isActive(true)
                        .build()
        ));

        String response = aiChatService.chat("session-1", 2L, "goi y 3 sach van hoc");

        assertTrue(response.contains("Tat den"));
        assertTrue(response.contains("So do"));
        assertTrue(response.contains("Frankenstein"));
        assertTrue(!response.contains("Truyen ke truoc gio ngu"));
        assertTrue(!response.contains("không có đủ sản phẩm"));
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenSemanticMatches_whenCatalogIntent_thenReturnCatalogBasedFallback() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("goi y sach van hoc", 5)).thenReturn(List.of(10L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Tat den")
                .author("Ngo Tat To")
                .description("Tieu thuyet hien thuc ve doi song nong dan Viet Nam.")
                .categoryNames(Set.of("Van hoc"))
                .quantity(7)
                .isActive(true)
                .build());

        String response = aiChatService.chat("session-1", 2L, "goi y sach van hoc");

        assertTrue(response.contains("Tat den"));
        assertTrue(response.contains("Ngo Tat To"));
        assertTrue(response.contains("Còn hàng: 7"));
        assertTrue(response.contains("/books/10"));
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenStrictTopicWithoutMatchingBooks_whenChat_thenReturnInsufficientProductsMessage() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("goi y 3 sach trinh tham", 5)).thenReturn(List.of(10L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Sach kinh te")
                .author("Tac gia A")
                .description("Sach ve quan ly tai chinh.")
                .quantity(7)
                .isActive(true)
                .build());

        String response = aiChatService.chat("session-1", 2L, "goi y 3 sach trinh tham");

        assertEquals("Hiện tại chúng tôi không có đủ sản phẩm cho yêu cầu của bạn.", response);
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenCatalogIntentWithoutAnyResult_whenChat_thenReturnInsufficientProductsMessageWithoutLlm() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("tim sach nau an", 5)).thenReturn(List.of());

        String response = aiChatService.chat("session-1", 2L, "tim sach nau an");

        assertEquals("Hiện tại chúng tôi không có đủ sản phẩm cho yêu cầu của bạn.", response);
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenCookingTopicWithUnrelatedSemanticMatch_whenChat_thenReturnInsufficientProductsMessage() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("tim sach nau an", 5)).thenReturn(List.of(10L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Sach lap trinh Java")
                .author("Tac gia A")
                .categoryNames(Set.of("Lap trinh"))
                .quantity(3)
                .isActive(true)
                .build());

        String response = aiChatService.chat("session-1", 2L, "tim sach nau an");

        assertEquals("Hiện tại chúng tôi không có đủ sản phẩm cho yêu cầu của bạn.", response);
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenHorrorTopicWithUnrelatedSemanticMatch_whenChat_thenReturnInsufficientProductsMessage() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("goi y 3 sach kinh di", 5)).thenReturn(List.of(10L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Frankenstein")
                .author("Mary Shelley")
                .categoryNames(Set.of("Van hoc"))
                .quantity(7)
                .isActive(true)
                .build());
        when(catalogBookPort.searchBooksByCategoryName("kinh di")).thenReturn(List.of());

        String response = aiChatService.chat("session-1", 2L, "goi y 3 sach kinh di");

        assertEquals("Hiện tại chúng tôi không có đủ sản phẩm cho yêu cầu của bạn.", response);
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenVagueCatalogKeywordWithUnrelatedSemanticMatches_whenChat_thenReturnInsufficientProductsMessage() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("sach ma", 5)).thenReturn(List.of(10L, 11L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Nguoi dieu khien me cung")
                .author("Donato Carrisi")
                .description("Tieu thuyet dieu tra ve cac vu mat tich.")
                .categoryNames(Set.of("Van hoc"))
                .quantity(26)
                .isActive(true)
                .build());
        when(catalogBookPort.getBook(11L)).thenReturn(BookContext.builder()
                .id(11L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .categoryNames(Set.of("Lap trinh"))
                .quantity(40)
                .isActive(true)
                .build());
        when(catalogBookPort.searchBooksByCategoryName("ma")).thenReturn(List.of());

        String response = aiChatService.chat("session-1", 2L, "sach ma");

        assertEquals("Hiện tại chúng tôi không có đủ sản phẩm cho yêu cầu của bạn.", response);
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenHotBooksIntent_whenChat_thenReturnSalesRankingBooksWithoutLlm() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(salesRankingPort.getTopSellingBooks(3)).thenReturn(List.of(
                BookContext.builder()
                        .id(10L)
                        .title("Top 1")
                        .author("Tac gia A")
                        .quantity(3)
                        .isActive(true)
                        .build(),
                BookContext.builder()
                        .id(11L)
                        .title("Top 2")
                        .author("Tac gia B")
                        .quantity(4)
                        .isActive(true)
                        .build(),
                BookContext.builder()
                        .id(12L)
                        .title("Top 3")
                        .author("Tac gia C")
                        .quantity(5)
                        .isActive(true)
                        .build()
        ));

        String response = aiChatService.chat("session-1", 2L, "goi y 3 sach hot");

        assertTrue(response.contains("Top 1"));
        assertTrue(response.contains("Top 2"));
        assertTrue(response.contains("Top 3"));
        assertTrue(response.contains("/books/10"));
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenAudienceIntent_whenChat_thenFilterByAudienceKeywordWithoutLlm() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("goi y 2 sach danh cho giao vien", 5)).thenReturn(List.of(10L, 11L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Phuong phap day hoc hien dai")
                .author("Tac gia A")
                .description("Tai lieu danh cho giao vien trung hoc.")
                .categoryNames(Set.of("Giao duc"))
                .quantity(5)
                .isActive(true)
                .build());
        when(catalogBookPort.getBook(11L)).thenReturn(BookContext.builder()
                .id(11L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .categoryNames(Set.of("Lap trinh"))
                .quantity(40)
                .isActive(true)
                .build());

        String response = aiChatService.chat("session-1", 2L, "goi y 2 sach danh cho giao vien");

        assertTrue(response.contains("Phuong phap day hoc hien dai"));
        assertTrue(!response.contains("Clean Code"));
        assertTrue(response.contains("không có đủ sản phẩm"));
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenJobAudienceIntentWithOnlyLooseDescriptionMatch_whenChat_thenReturnInsufficientProductsMessage() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("sach danh cho bac si", 5)).thenReturn(List.of(10L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Nguoi dieu khien me cung")
                .author("Donato Carrisi")
                .description("Cau chuyen dieu tra co nhan vat bac si va mot benh vien bo hoang.")
                .categoryNames(Set.of("Trinh tham"))
                .quantity(26)
                .isActive(true)
                .build());

        String response = aiChatService.chat("session-1", 2L, "sach danh cho bac si");

        assertEquals("Hiện tại chúng tôi không có đủ sản phẩm cho yêu cầu của bạn.", response);
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenJobAudienceIntent_whenChat_thenExpandJobKeywordsWithoutLlm() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("goi y sach danh cho dau bep", 5)).thenReturn(List.of(10L, 11L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Bi quyet nau an gia dinh")
                .author("Tac gia A")
                .description("Huong dan xay dung thuc don va che bien mon an hang ngay.")
                .categoryNames(Set.of("Am thuc"))
                .quantity(5)
                .isActive(true)
                .build());
        when(catalogBookPort.getBook(11L)).thenReturn(BookContext.builder()
                .id(11L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .categoryNames(Set.of("Lap trinh"))
                .quantity(40)
                .isActive(true)
                .build());

        String response = aiChatService.chat("session-1", 2L, "goi y sach danh cho dau bep");

        assertTrue(response.contains("Bi quyet nau an gia dinh"));
        assertTrue(!response.contains("Clean Code"));
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenAudienceIntentWithoutRequestedCount_whenChat_thenDoNotShowInsufficientProductsMessage() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("sach danh cho ky su phan mem", 5)).thenReturn(List.of(10L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Clean Code - Ban thuc hanh")
                .author("Robert C. Martin")
                .categoryNames(Set.of("Lap trinh"))
                .keywords(Set.of("lap trinh", "code"))
                .quantity(40)
                .isActive(true)
                .build());

        String response = aiChatService.chat("session-1", 2L, "sach danh cho ky su phan mem");

        assertTrue(response.contains("Clean Code - Ban thuc hanh"));
        assertTrue(!response.contains("không có đủ sản phẩm"));
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenAudienceIntentWithoutMatch_whenChat_thenReturnInsufficientProductsMessage() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("sach danh cho dau bep", 5)).thenReturn(List.of(10L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .categoryNames(Set.of("Lap trinh"))
                .quantity(40)
                .isActive(true)
                .build());

        String response = aiChatService.chat("session-1", 2L, "sach danh cho dau bep");

        assertEquals("Hiện tại chúng tôi không có đủ sản phẩm cho yêu cầu của bạn.", response);
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenAuthorIntent_whenChat_thenFilterByAuthorWithoutLlm() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("sach cua tac gia Mary Shelley", 5)).thenReturn(List.of(10L, 11L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Frankenstein")
                .author("Mary Shelley")
                .categoryNames(Set.of("Van hoc"))
                .quantity(7)
                .isActive(true)
                .build());
        when(catalogBookPort.getBook(11L)).thenReturn(BookContext.builder()
                .id(11L)
                .title("Oliver Twist")
                .author("Charles Dickens")
                .categoryNames(Set.of("Van hoc"))
                .quantity(2)
                .isActive(true)
                .build());

        String response = aiChatService.chat("session-1", 2L, "sach cua tac gia Mary Shelley");

        assertTrue(response.contains("Frankenstein"));
        assertTrue(!response.contains("Oliver Twist"));
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenStrictTopicMatchesCategoryName_whenChat_thenUseExistingBook() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("goi y cho toi 1 quyen sach thuoc the loai trinh tham", 5))
                .thenReturn(List.of(10L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("An mang tren song Nile")
                .author("Agatha Christie")
                .description("Tieu thuyet dieu tra kinh dien.")
                .categoryNames(Set.of("Trinh tham"))
                .quantity(1)
                .isActive(true)
                .build());

        String response = aiChatService.chat(
                "session-1",
                2L,
                "goi y cho toi 1 quyen sach thuoc the loai trinh tham"
        );

        assertTrue(response.contains("An mang tren song Nile"));
        assertTrue(response.contains("/books/10"));
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenTitleSearchIntentWithCategoryWord_whenChat_thenFilterByTitleNotCategory() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("tim sach co chu thieu nhi trong ten", 5))
                .thenReturn(List.of(10L, 11L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Van hoc thieu nhi Viet Nam")
                .author("Tac gia A")
                .categoryNames(Set.of("Van hoc"))
                .quantity(1)
                .isActive(true)
                .build());
        when(catalogBookPort.getBook(11L)).thenReturn(BookContext.builder()
                .id(11L)
                .title("Truyen ke truoc gio ngu")
                .author("SEBook Kids")
                .categoryNames(Set.of("Thieu nhi"))
                .quantity(60)
                .isActive(true)
                .build());

        String response = aiChatService.chat("session-1", 2L, "tim sach co chu thieu nhi trong ten");

        assertTrue(response.contains("Van hoc thieu nhi Viet Nam"));
        assertTrue(response.contains("/books/10"));
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenTitleSearchIntentWithUnknownTermWithoutAnyResult_whenChat_thenReturnInsufficientProductsWithoutLlm() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("3 sach co chu vo thuat trong ten", 5)).thenReturn(List.of());

        String response = aiChatService.chat("session-1", 2L, "3 sach co chu vo thuat trong ten");

        assertEquals("Hiện tại chúng tôi không có đủ sản phẩm cho yêu cầu của bạn.", response);
        verify(llmPort, never()).chat(any(), any());
    }

    @Test
    void givenRequestedBookCount_whenCatalogIntent_thenLimitCatalogFallbackToRequestedCount() {
        when(historyPort.findById("session-1")).thenReturn(Optional.empty());
        when(historyPort.findMessagesBySessionId("session-1")).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks("goi y 2 quyen sach van hoc", 5)).thenReturn(List.of(10L, 11L, 12L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Tat den")
                .author("Ngo Tat To")
                .categoryNames(Set.of("Van hoc"))
                .quantity(7)
                .isActive(true)
                .build());
        when(catalogBookPort.getBook(11L)).thenReturn(BookContext.builder()
                .id(11L)
                .title("So do")
                .author("Vu Trong Phung")
                .categoryNames(Set.of("Van hoc"))
                .quantity(5)
                .isActive(true)
                .build());
        when(catalogBookPort.getBook(12L)).thenReturn(BookContext.builder()
                .id(12L)
                .title("Lao Hac")
                .author("Nam Cao")
                .categoryNames(Set.of("Van hoc"))
                .quantity(3)
                .isActive(true)
                .build());

        String response = aiChatService.chat("session-1", 2L, "goi y 2 quyen sach van hoc");

        assertTrue(response.contains("Tat den"));
        assertTrue(response.contains("/books/10"));
        assertTrue(response.contains("So do"));
        assertTrue(response.contains("Vu Trong Phung"));
        assertTrue(response.contains("/books/11"));
        assertTrue(!response.contains("Lao Hac"));
        verify(llmPort, never()).chat(any(), any());
    }
}
