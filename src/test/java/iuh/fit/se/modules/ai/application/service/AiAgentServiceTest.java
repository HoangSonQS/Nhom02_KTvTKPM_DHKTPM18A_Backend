package iuh.fit.se.modules.ai.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.modules.ai.application.port.in.AiAgentUseCase;
import iuh.fit.se.modules.ai.application.port.in.AiChatUseCase;
import iuh.fit.se.modules.ai.application.port.out.*;
import iuh.fit.se.modules.ai.domain.AiAgentIntent;
import iuh.fit.se.modules.ai.domain.AiAgentPendingAction;
import iuh.fit.se.modules.cart.application.port.in.CartInternalUseCase;
import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase;
import iuh.fit.se.modules.payment.application.port.in.PaymentUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAgentServiceTest {

    @Mock
    private LlmPort llmPort;
    @Mock
    private ChatHistoryPersistencePort historyPort;
    @Mock
    private VectorStorePort vectorStorePort;
    @Mock
    private CatalogBookPort catalogBookPort;
    @Mock
    private CartInternalUseCase cartUseCase;
    @Mock
    private OrderInternalUseCase orderUseCase;
    @Mock
    private PaymentUseCase paymentUseCase;
    @Mock
    private AiAgentPendingActionPersistencePort pendingActionPort;
    @Mock
    private AiChatUseCase chatUseCase;
    @Mock
    private AiCheckoutDraftPersistencePort checkoutDraftPort;
    @Mock
    private AiSearchContextPersistencePort searchContextPort;
    @Mock
    private AiBookContextPersistencePort bookContextPort;

    private AiAgentService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        service = new AiAgentService(
                chatUseCase,
                historyPort,
                vectorStorePort,
                catalogBookPort,
                cartUseCase,
                orderUseCase,
                paymentUseCase,
                pendingActionPort,
                checkoutDraftPort,
                searchContextPort,
                bookContextPort,
                objectMapper,
                new AiAgentRuleEngine(),
                new AiAgentValidator(),
                new AiAgentResponseFactory()
        );
        lenient().when(historyPort.findById(anyString())).thenReturn(Optional.empty());
        lenient().when(checkoutDraftPort.findBySessionId(anyString())).thenReturn(Optional.empty());
        lenient().when(searchContextPort.findBySessionId(anyString())).thenReturn(Optional.empty());
        lenient().when(bookContextPort.findBySessionId(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void givenAddToCartIntent_whenHandleMessage_thenAddToCartImmediately() {
        when(vectorStorePort.findSimilarBooks(anyString(), anyInt())).thenReturn(List.of(10L));
        when(catalogBookPort.getBook(10L)).thenReturn(book(10L));
        when(cartUseCase.getCartByUserId(5L)).thenReturn(cartWithItem(10L, "Đắc Nhân Tâm", 2));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-1", 5L, "thêm 2 cuốn Đắc Nhân Tâm vào giỏ")
        );

        assertThat(response.confirmationCard()).isNull();
        assertThat(response.message()).contains("Đắc Nhân Tâm");
        verify(cartUseCase).addItem(eq(5L), any(CartInternalUseCase.AddItemCommand.class));

        verify(pendingActionPort, never()).save(any());
        verifyNoInteractions(chatUseCase);
    }

    @Test
    void givenCheckoutIntentMissingAddress_whenHandleMessage_thenAskForMoreInfoAndDoNotCreateOrder() {
        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-2", 5L, "đặt đơn này COD")
        );

        assertThat(response.message()).contains("địa chỉ giao hàng");
        assertThat(response.confirmationCard()).isNull();
        verify(orderUseCase, never()).checkout(anyLong(), any());
        verify(pendingActionPort, never()).save(any());
    }

    @Test
    void givenLowConfidenceIntent_whenHandleMessage_thenDelegateToChatbot() {
        when(chatUseCase.chat(eq("session-3"), eq(5L), anyString()))
                .thenReturn("M\u00ecnh ch\u01b0a ch\u1eafc, b\u1ea1n n\u00f3i r\u00f5 h\u01a1n nh\u00e9.");

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-3", 5L, "ừm cái đó á")
        );

        assertThat(response.message()).contains("chưa chắc");
        verifyNoInteractions(cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenRuleMiss_whenHandleMessage_thenDelegateToChatbotOnceWithoutAgentHistoryWrites() {
        when(chatUseCase.chat("session-chatbot", 5L, "Xin chao"))
                .thenReturn("Xin chao, minh co the giup ban tim sach.");

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-chatbot", 5L, "Xin chao")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.UNKNOWN);
        assertThat(response.message()).isEqualTo("Xin chao, minh co the giup ban tim sach.");
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verify(chatUseCase).chat("session-chatbot", 5L, "Xin chao");
        verify(historyPort, never()).saveMessage(any());
        verifyNoInteractions(llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenBroadAudienceRecommendation_whenHandleMessage_thenDelegateToChatbot() {
        when(chatUseCase.chat("session-student", 5L, "Sach nao phu hop cho sinh vien?"))
                .thenReturn("Ban dang hoc nganh nao de minh goi y sat hon?");

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-student", 5L, "Sach nao phu hop cho sinh vien?")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.UNKNOWN);
        assertThat(response.message()).isEqualTo("Ban dang hoc nganh nao de minh goi y sat hon?");
        verify(chatUseCase).chat("session-student", 5L, "Sach nao phu hop cho sinh vien?");
        verify(historyPort, never()).saveMessage(any());
        verifyNoInteractions(catalogBookPort, vectorStorePort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenStockQuestion_whenHandleMessage_thenReturnBookStock() {
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(10L)
                        .title("Clean Code")
                        .author("Robert C. Martin")
                        .build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(120000))
                .quantity(10)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-stock", 5L, "sách clean code còn hàng không")
        );

        assertThat(response.message()).contains("Clean Code");
        assertThat(response.message()).contains("10");
        assertThat(response.books()).hasSize(1);
        assertThat(response.books().get(0).bookId()).isEqualTo(10L);
        verifyNoInteractions(cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenPriceQuestion_whenHandleMessage_thenReturnBookDetailWithoutWriteAction() {
        when(catalogBookPort.searchBooks(eq("atomic habits"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(20L)
                        .title("Atomic Habits")
                        .author("James Clear")
                        .build()
        ));
        when(catalogBookPort.getBook(20L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(20L)
                .title("Atomic Habits")
                .author("James Clear")
                .description("Sach ve thoi quen va ky nang song")
                .price(BigDecimal.valueOf(189000))
                .quantity(20)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-price-detail", 5L, "Sach Atomic Habits gia bao nhieu?")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.VIEW_BOOK_DETAIL);
        assertThat(response.books()).hasSize(1);
        assertThat(response.books().get(0).price()).isEqualByComparingTo(BigDecimal.valueOf(189000));
        verifyNoInteractions(llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenViewDetailQuestion_whenHandleMessage_thenReturnBookDetailWithoutWriteAction() {
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(10L)
                        .title("Clean Code")
                        .author("Robert C. Martin")
                        .build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .description("Sach ve code sach")
                .price(BigDecimal.valueOf(350000))
                .quantity(15)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-book-detail", 5L, "Cho toi xem chi tiet sach Clean Code")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.VIEW_BOOK_DETAIL);
        assertThat(response.books()).hasSize(1);
        assertThat(response.books().get(0).title()).isEqualTo("Clean Code");
        verifyNoInteractions(llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenAuthorQuestion_whenHandleMessage_thenReturnBookDetailWithoutWriteAction() {
        when(catalogBookPort.searchBooks(eq("dac nhan tam"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(30L)
                        .title("Dac Nhan Tam")
                        .author("Dale Carnegie")
                        .build()
        ));
        when(catalogBookPort.getBook(30L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(30L)
                .title("Dac Nhan Tam")
                .author("Dale Carnegie")
                .description("Sach giao tiep kinh dien")
                .price(BigDecimal.valueOf(99000))
                .quantity(30)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-author-detail", 5L, "Quyen Dac nhan tam cua tac gia nao?")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.VIEW_BOOK_DETAIL);
        assertThat(response.books()).hasSize(1);
        assertThat(response.books().get(0).author()).isEqualTo("Dale Carnegie");
        verifyNoInteractions(llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenPurchaseStockQuestion_whenHandleMessage_thenOnlyCheckStockAndDoNotCreatePendingOrder() {
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(10L)
                        .title("Clean Code")
                        .author("Robert C. Martin")
                        .build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(350000))
                .quantity(15)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-purchase-stock", 5L, "Toi muon mua 3 cuon Clean Code, con du khong?")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.CHECK_STOCK);
        assertThat(response.message()).contains("Clean Code");
        assertThat(response.message()).contains("15");
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verifyNoInteractions(llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenClearSearchBookRequest_whenHandleMessage_thenReturnBookCardsWithoutCreatingOrder() {
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(10L)
                        .title("Clean Code")
                        .author("Robert C. Martin")
                        .build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(120000))
                .quantity(10)
                .imageUrl("https://cdn.test/clean-code.jpg")
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-search", 5L, "Tìm giúp tôi sách Clean Code")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.SEARCH_BOOK);
        assertThat(response.books()).hasSize(1);
        assertThat(response.cards()).hasSize(1);
        assertThat(response.cards().get(0).type()).isEqualTo("BOOK_CARD");
        verifyNoInteractions(llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenRecommendationQuestion_whenHandleMessage_thenReturnBookCardsWithoutCreatingOrder() {
        when(catalogBookPort.searchBooks(eq("ky nang song"), isNull())).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks(eq("ky nang song"), anyInt())).thenReturn(List.of(20L));
        when(catalogBookPort.getBook(20L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(20L)
                .title("Đắc Nhân Tâm")
                .author("Dale Carnegie")
                .description("Sách kỹ năng sống kinh điển")
                .price(BigDecimal.valueOf(99000))
                .quantity(5)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-recommend", 5L, "Có quyển nào về kỹ năng sống không?")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.RECOMMEND_BOOK);
        assertThat(response.books()).hasSize(1);
        assertThat(response.cards()).hasSize(1);
        verifyNoInteractions(llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenAuthorSearchRequest_whenHandleMessage_thenReturnBookCardsWithoutCreatingOrder() {
        when(catalogBookPort.searchBooks(eq("nguyen nhat anh"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(30L)
                        .title("Cho Tôi Xin Một Vé Đi Tuổi Thơ")
                        .author("Nguyễn Nhật Ánh")
                        .build()
        ));
        when(catalogBookPort.getBook(30L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(30L)
                .title("Cho Tôi Xin Một Vé Đi Tuổi Thơ")
                .author("Nguyễn Nhật Ánh")
                .price(BigDecimal.valueOf(85000))
                .quantity(7)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-author", 5L, "Tôi muốn tìm sách của Nguyễn Nhật Ánh")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.SEARCH_BOOK);
        assertThat(response.books()).hasSize(1);
        assertThat(response.cards()).hasSize(1);
        verifyNoInteractions(llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenMissingAuthorSearchAndVectorHasUnrelatedBooks_whenHandleMessage_thenDoNotReturnUnrelatedBooks() {
        when(catalogBookPort.searchBooks(eq("nguyen nhat anh"), isNull())).thenReturn(List.of());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-author-missing", 5L, "Tôi muốn tìm sách của Nguyễn Nhật Ánh")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.SEARCH_BOOK);
        assertThat(response.books()).isNull();
        assertThat(response.cards()).isNull();
        assertThat(response.message()).contains("chưa tìm thấy");
        verifyNoInteractions(vectorStorePort, llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenMissingSpecificBookQuestion_whenHandleMessage_thenReturnNotFoundInsteadOfClarification() {
        when(catalogBookPort.searchBooks(eq("atomic habits"), isNull())).thenReturn(List.of());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-missing-book", 5L, "Có sách Atomic Habits không?")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.SEARCH_BOOK);
        assertThat(response.message()).contains("chưa tìm thấy");
        verifyNoInteractions(vectorStorePort, llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenRecommendationQuestionAndVectorReturnsIrrelevantBooks_whenHandleMessage_thenDoNotReturnIrrelevantCards() {
        when(catalogBookPort.searchBooks(eq("ky nang song"), isNull())).thenReturn(List.of());
        when(catalogBookPort.searchBooks(isNull(), isNull())).thenReturn(List.of());
        when(vectorStorePort.findSimilarBooks(eq("ky nang song"), anyInt())).thenReturn(List.of(10L, 11L));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .description("Sách về lập trình phần mềm")
                .price(BigDecimal.valueOf(120000))
                .quantity(10)
                .isActive(true)
                .build());
        when(catalogBookPort.getBook(11L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(11L)
                .title("Quyển test")
                .author("Test")
                .description("Dữ liệu test")
                .price(BigDecimal.valueOf(10000))
                .quantity(10)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-recommend-irrelevant", 5L, "Có quyển nào về kỹ năng sống không?")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.RECOMMEND_BOOK);
        assertThat(response.books()).isNull();
        assertThat(response.cards()).isNull();
        assertThat(response.message()).contains("chưa tìm thấy");
        verifyNoInteractions(llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenTopicRecommendationAndCatalogHasKeywordMatch_whenHandleMessage_thenReturnMatchingCardsWithoutVectorSync() {
        when(catalogBookPort.searchBooks(eq("ky nang song"), isNull())).thenReturn(List.of());
        when(catalogBookPort.searchBooks(isNull(), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder().id(20L).title("Atomic Habits").build(),
                CatalogBookPort.BookDocument.builder().id(21L).title("Clean Code").build()
        ));
        when(catalogBookPort.getBook(20L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(20L)
                .title("Atomic Habits")
                .author("James Clear")
                .description("Sach ve ky nang song va thoi quen nho tao thay doi lon")
                .keywords(java.util.Set.of("ky nang song", "thoi quen", "phat trien ban than"))
                .price(BigDecimal.valueOf(189000))
                .quantity(20)
                .isActive(true)
                .build());
        when(catalogBookPort.getBook(21L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(21L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .description("Sach ve lap trinh phan mem")
                .keywords(java.util.Set.of("lap trinh", "clean code"))
                .price(BigDecimal.valueOf(350000))
                .quantity(15)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-topic-catalog", 5L, "Co quyen nao ve ky nang song khong?")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.RECOMMEND_BOOK);
        assertThat(response.books()).hasSize(1);
        assertThat(response.books().get(0).title()).isEqualTo("Atomic Habits");
        verifyNoInteractions(vectorStorePort, llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenJavaProgrammingSearchAndCatalogHasKeywordMatch_whenHandleMessage_thenReturnJavaBook() {
        when(catalogBookPort.searchBooks(eq("lap trinh java"), isNull())).thenReturn(List.of());
        when(catalogBookPort.searchBooks(isNull(), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder().id(40L).title("Effective Java").build(),
                CatalogBookPort.BookDocument.builder().id(41L).title("Clean Code").build()
        ));
        when(catalogBookPort.getBook(40L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(40L)
                .title("Effective Java")
                .author("Joshua Bloch")
                .description("Sach lap trinh Java ve API design, generics, concurrency va best practices")
                .keywords(java.util.Set.of("lap trinh java", "java", "backend"))
                .price(BigDecimal.valueOf(420000))
                .quantity(12)
                .isActive(true)
                .build());
        when(catalogBookPort.getBook(41L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(41L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .description("Sach ve lap trinh phan mem")
                .keywords(java.util.Set.of("lap trinh", "clean code"))
                .price(BigDecimal.valueOf(350000))
                .quantity(15)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-java-search", 5L, "Tim sach lap trinh Java")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.SEARCH_BOOK);
        assertThat(response.books()).hasSize(1);
        assertThat(response.books().get(0).title()).isEqualTo("Effective Java");
        verifyNoInteractions(vectorStorePort, llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenProgrammingSearch_whenAskForOtherBooks_thenReturnNextBatchWithoutRepeatingPreviousBooks() {
        List<CatalogBookPort.BookDocument> documents = java.util.stream.LongStream.rangeClosed(1, 10)
                .mapToObj(id -> CatalogBookPort.BookDocument.builder().id(id).title("Programming Book " + id).build())
                .toList();
        when(catalogBookPort.searchBooks(eq("lap trinh"), isNull())).thenReturn(documents);
        for (long id = 1; id <= 10; id++) {
            when(catalogBookPort.getBook(id)).thenReturn(CatalogBookPort.BookContext.builder()
                    .id(id)
                    .title("Programming Book " + id)
                    .description("Sach ve lap trinh phan mem")
                    .keywords(java.util.Set.of("lap trinh"))
                    .quantity(10)
                    .isActive(true)
                    .build());
        }

        AiAgentUseCase.AgentResponse firstResponse = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-programming-more", 5L, "Tim sach ve lap trinh")
        );
        AiAgentUseCase.AgentResponse nextResponse = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-programming-more", 5L, "Tim sach khac")
        );

        assertThat(firstResponse.books()).extracting(AiAgentUseCase.BookResult::bookId)
                .containsExactly(1L, 2L, 3L, 4L, 5L);
        assertThat(nextResponse.books()).extracting(AiAgentUseCase.BookResult::bookId)
                .containsExactly(6L, 7L, 8L, 9L, 10L);
    }

    @Test
    void givenSharedProgrammingSearchContext_whenRequestHitsAnotherInstance_thenReturnNextBatch() {
        when(searchContextPort.findBySessionId("session-programming-shared"))
                .thenReturn(Optional.of(new AiSearchContextPersistencePort.SearchContext(
                        "lap trinh",
                        java.util.Set.of(1L, 2L, 3L, 4L, 5L)
                )));
        List<CatalogBookPort.BookDocument> documents = java.util.stream.LongStream.rangeClosed(1, 10)
                .mapToObj(id -> CatalogBookPort.BookDocument.builder().id(id).title("Programming Book " + id).build())
                .toList();
        when(catalogBookPort.searchBooks(eq("lap trinh"), isNull())).thenReturn(documents);
        for (long id = 1; id <= 10; id++) {
            when(catalogBookPort.getBook(id)).thenReturn(CatalogBookPort.BookContext.builder()
                    .id(id)
                    .title("Programming Book " + id)
                    .description("Sach ve lap trinh phan mem")
                    .keywords(java.util.Set.of("lap trinh"))
                    .quantity(10)
                    .isActive(true)
                    .build());
        }

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-programming-shared", 5L, "Tim sach khac")
        );

        assertThat(response.books()).extracting(AiAgentUseCase.BookResult::bookId)
                .containsExactly(6L, 7L, 8L, 9L, 10L);
    }

    @Test
    void givenSimilarBookSearch_whenHandleMessage_thenStripSimilarityWordsAndReturnBookCards() {
        when(catalogBookPort.searchBooks(eq("nha gia kim"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder().id(50L).title("Nha Gia Kim").build()
        ));
        when(catalogBookPort.getBook(50L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(50L)
                .title("Nha Gia Kim")
                .author("Paulo Coelho")
                .description("Tieu thuyet truyen cam hung ve uoc mo va hanh trinh truong thanh")
                .keywords(java.util.Set.of("van hoc", "truyen cam hung", "uoc mo"))
                .price(BigDecimal.valueOf(89000))
                .quantity(25)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-similar-book", 5L, "Tim sach giong Nha Gia Kim")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.RECOMMEND_BOOK);
        assertThat(response.books()).hasSize(1);
        assertThat(response.books().get(0).title()).isEqualTo("Nha Gia Kim");
        verifyNoInteractions(vectorStorePort, llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenPurchaseRequestAndLlmUnavailable_whenHandleMessage_thenAskForCheckoutInfoWithBookCard() {
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(10L)
                        .title("Clean Code")
                        .author("Robert C. Martin")
                        .build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(120000))
                .quantity(10)
                .imageUrl("https://cdn.test/clean-code.jpg")
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-purchase", 5L, "đặt cho tôi 1 cuốn clean code")
        );

        assertThat(response.message()).contains("Clean Code");
        assertThat(response.message()).contains("địa chỉ giao hàng");
        assertThat(response.confirmationCard()).isNull();
        assertThat(response.books()).hasSize(1);
        assertThat(response.books().get(0).imageUrl()).isEqualTo("https://cdn.test/clean-code.jpg");
        verify(orderUseCase, never()).checkout(anyLong(), any());
        verify(pendingActionPort, never()).save(any());
    }

    @Test
    void givenAmbiguousPurchaseQuestion_whenHandleMessage_thenDoNotCreatePendingAction() {
        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-ambiguous", 5L, "Tôi đang phân vân có nên mua Clean Code không")
        );

        assertThat(response.confirmationCard()).isNull();
        assertThat(response.pendingAction()).isNull();
        verify(orderUseCase, never()).checkout(anyLong(), any());
        verify(pendingActionPort, never()).save(any());
    }

    @Test
    void givenAdvicePurchaseQuestionWithBook_whenHandleMessage_thenReturnBookDetailWithoutPendingOrder() {
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(10L)
                        .title("Clean Code")
                        .author("Robert C. Martin")
                        .build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .description("Practical advice for writing maintainable code")
                .price(BigDecimal.valueOf(350000))
                .quantity(12)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-advice-clean-code", 5L,
                        "Toi dang phan van co nen mua Clean Code khong")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.VIEW_BOOK_DETAIL);
        assertThat(response.books()).hasSize(1);
        assertThat(response.books().get(0).title()).isEqualTo("Clean Code");
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verifyNoInteractions(llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenWorthBuyingQuestionWithBook_whenHandleMessage_thenReturnBookDetailWithoutPendingOrder() {
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder().id(10L).title("Clean Code").author("Robert C. Martin").build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(350000))
                .quantity(12)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-worth-clean-code", 5L,
                        "Clean Code co dang mua khong?")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.VIEW_BOOK_DETAIL);
        assertThat(response.books()).hasSize(1);
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verifyNoInteractions(llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenPreviewBookQuestion_whenHandleMessage_thenReturnBookDetailWithoutPendingOrder() {
        when(catalogBookPort.searchBooks(eq("atomic habits"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder().id(20L).title("Atomic Habits").author("James Clear").build()
        ));
        when(catalogBookPort.getBook(20L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(20L)
                .title("Atomic Habits")
                .author("James Clear")
                .price(BigDecimal.valueOf(189000))
                .quantity(8)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-preview-atomic", 5L,
                        "Toi muon xem thu quyen Atomic Habits")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.VIEW_BOOK_DETAIL);
        assertThat(response.books()).hasSize(1);
        assertThat(response.books().get(0).title()).isEqualTo("Atomic Habits");
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verifyNoInteractions(llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenAmbiguousFutureOrderQuestionWithoutBook_whenHandleMessage_thenAnswerNaturallyWithoutPendingOrder() {
        when(chatUseCase.chat(eq("session-future-order"), eq(5L), anyString()))
                .thenReturn("B\u1ea1n cho m\u00ecnh bi\u1ebft t\u00ean s\u00e1ch mu\u1ed1n ki\u1ec3m tra nh\u00e9.");

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-future-order", 5L,
                        "Neu con hang thi toi tinh dat sau")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.UNKNOWN);
        assertThat(response.message()).contains("tên sách");
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verify(orderUseCase, never()).checkout(anyLong(), any());
        verify(pendingActionPort, never()).save(any());
    }

    @Test
    void givenKeywordAppearsInQuestionOnly_whenHandleMessage_thenDoNotClassifyAsWriteAction() {
        List<String> messages = List.of(
                "Toi nen mua Clean Code hay Code Complete?",
                "Toi da mua quyen nay chua?",
                "Lam sao de dat hang tren website?",
                "Neu toi them sach vao gio thi co giu hang khong?",
                "Toi muon hoi cach huy don hang"
        );
        when(chatUseCase.chat(anyString(), eq(5L), anyString()))
                .thenReturn("M\u00ecnh s\u1ebd h\u01b0\u1edbng d\u1eabn b\u1ea1n.");

        for (int i = 0; i < messages.size(); i++) {
            AiAgentUseCase.AgentResponse response = service.handleMessage(
                    new AiAgentUseCase.AgentMessageCommand("session-keyword-question-" + i, 5L, messages.get(i))
            );

            assertThat(response.intent()).isNotIn(
                    AiAgentIntent.ADD_TO_CART,
                    AiAgentIntent.PLACE_ORDER,
                    AiAgentIntent.CANCEL_ORDER
            );
            assertThat(response.pendingAction()).isNull();
            assertThat(response.confirmationCard()).isNull();
        }
        verify(chatUseCase).chat("session-keyword-question-2", 5L, "Lam sao de dat hang tren website?");
        verify(chatUseCase).chat("session-keyword-question-3", 5L, "Neu toi them sach vao gio thi co giu hang khong?");
        verify(chatUseCase).chat("session-keyword-question-4", 5L, "Toi muon hoi cach huy don hang");
        verifyNoInteractions(cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenAddToCartWithZeroQuantity_whenHandleMessage_thenRejectInvalidQuantityAndDoNotWriteCart() {
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder().id(10L).title("Clean Code").author("Robert C. Martin").build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(bookContext(10L, "Clean Code", "Robert C. Martin"));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-invalid-zero", 5L, "Them 0 cuon Clean Code vao gio")
        );

        assertThat(response.error().code()).isEqualTo("INVALID_QUANTITY");
        assertThat(response.message()).contains("kh\u00f4ng h\u1ee3p l\u1ec7");
        verify(cartUseCase, never()).addItem(anyLong(), any());
        verifyNoInteractions(orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenPlaceOrderWithNegativeQuantity_whenHandleMessage_thenRejectInvalidQuantityBeforeAskingCheckoutInfo() {
        when(catalogBookPort.searchBooks(eq("atomic habits"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder().id(20L).title("Atomic Habits").author("James Clear").build()
        ));
        when(catalogBookPort.getBook(20L)).thenReturn(bookContext(20L, "Atomic Habits", "James Clear"));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-invalid-negative", 5L, "Dat -1 cuon Atomic Habits")
        );

        assertThat(response.error().code()).isEqualTo("INVALID_QUANTITY");
        assertThat(response.message()).contains("l\u1edbn h\u01a1n 0");
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verifyNoInteractions(cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenPlaceOrderQuantityBeyondStock_whenHandleMessage_thenRejectInsufficientStockBeforeCreatingPendingAction() {
        when(catalogBookPort.searchBooks(eq("dac nhan tam"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder().id(30L).title("Dac Nhan Tam").author("Dale Carnegie").build()
        ));
        when(catalogBookPort.getBook(30L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(30L)
                .title("Dac Nhan Tam")
                .author("Dale Carnegie")
                .price(BigDecimal.valueOf(99000))
                .quantity(3)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-insufficient-stock", 5L, "Mua 9999 cuon Dac nhan tam")
        );

        assertThat(response.error().code()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(response.message()).contains("Kh\u00f4ng \u0111\u1ee7 h\u00e0ng");
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verifyNoInteractions(cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenVietnameseWordQuantity_whenHandleMessage_thenConvertAndValidateStock() {
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder().id(10L).title("Clean Code").author("Robert C. Martin").build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(120000))
                .quantity(10)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-word-quantity", 5L, "Them hai muoi cuon Clean Code vao gio")
        );

        assertThat(response.error().code()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(response.message()).contains("ch\u1ec9 c\u00f2n 10");
        verify(cartUseCase, never()).addItem(anyLong(), any());
        verifyNoInteractions(orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenAddToCartWithoutBook_whenHandleMessage_thenAskWhichBookAndDoNotWriteCart() {
        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-missing-cart-book", 5L, "Them vao gio")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.ADD_TO_CART);
        assertThat(response.message()).contains("sách cần thao tác");
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verifyNoInteractions(catalogBookPort, llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenPurchaseWithoutBook_whenHandleMessage_thenAskWhichBookAndDoNotCreatePendingOrder() {
        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-missing-order-book", 5L, "Mua 2 cuon")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.PLACE_ORDER);
        assertThat(response.message()).contains("sách cần thao tác");
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verifyNoInteractions(catalogBookPort, llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenGenericOrderRequestWithoutBook_whenHandleMessage_thenAskWhichBookAndDoNotCreatePendingOrder() {
        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-generic-order-book", 5L, "Toi muon dat sach")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.PLACE_ORDER);
        assertThat(response.message()).contains("sách cần thao tác");
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verifyNoInteractions(catalogBookPort, llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenReferenceBookWithoutContext_whenHandleMessage_thenAskWhichBookAndDoNotCreatePendingOrder() {
        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-reference-order-book", 5L, "Lay cuon dau tien")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.PLACE_ORDER);
        assertThat(response.message()).contains("sách cần thao tác");
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verifyNoInteractions(catalogBookPort, llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenOrderThatBookWithoutContext_whenHandleMessage_thenAskWhichBookAndDoNotCreatePendingOrder() {
        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-that-book-order", 5L, "Dat cho toi cuon do")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.PLACE_ORDER);
        assertThat(response.message()).contains("sách cần thao tác");
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verifyNoInteractions(catalogBookPort, llmPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenPreviousSingleBookContext_whenOrderThatBookAndThenProvideCheckoutInfo_thenCreatePendingForThatBook() throws Exception {
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder().id(10L).title("Clean Code").author("Robert C. Martin").build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(350000))
                .quantity(15)
                .isActive(true)
                .build());
        when(pendingActionPort.save(any(AiAgentPendingAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiAgentUseCase.AgentResponse stockResponse = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-context-book", 5L, "sach clean code con hang khong")
        );
        AiAgentUseCase.AgentResponse missingInfoResponse = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-context-book", 5L, "dat cho toi cuon do")
        );
        AiAgentUseCase.AgentResponse checkoutResponse = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-context-book", 5L,
                        "Dat hang COD; dia chi giao hang: 12 nvb, An Bien, An Giang; so dien thoai: 0788912345")
        );

        ArgumentCaptor<AiAgentPendingAction> captor = ArgumentCaptor.forClass(AiAgentPendingAction.class);
        verify(pendingActionPort).save(captor.capture());
        AiAgentService.ActionPayload payload = new ObjectMapper()
                .readValue(captor.getValue().getPayload(), AiAgentService.ActionPayload.class);

        assertThat(stockResponse.books()).hasSize(1);
        assertThat(missingInfoResponse.message()).doesNotContain("sách cần thao tác");
        assertThat(missingInfoResponse.books()).hasSize(1);
        assertThat(missingInfoResponse.checkoutScope()).isEqualTo("BOOK");
        assertThat(checkoutResponse.pendingAction()).isNotNull();
        assertThat(checkoutResponse.confirmationCard().description()).contains("Clean Code");
        assertThat(checkoutResponse.confirmationCard().description()).doesNotContain("giỏ hàng");
        assertThat(payload.bookId()).isEqualTo(10L);
        assertThat(payload.bookTitle()).isEqualTo("Clean Code");
        assertThat(payload.selectedBookIds()).containsExactly(10L);
        assertThat(payload.cartCheckout()).isFalse();
        verify(orderUseCase, never()).checkout(anyLong(), any());
    }

    @Test
    void givenBookContextFromAnotherInstance_whenOrderThatBook_thenUseRedisContext() {
        when(bookContextPort.findBySessionId("session-redis-book-context")).thenReturn(Optional.of(
                new AiBookContextPersistencePort.BookContext(List.of(
                        new AiBookContextPersistencePort.BookReference(10L, "Clean Code")
                ))
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(350000))
                .quantity(15)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-redis-book-context", 5L, "Dat cho toi cuon do")
        );

        assertThat(response.books()).hasSize(1);
        assertThat(response.books().get(0).bookId()).isEqualTo(10L);
        assertThat(response.checkoutScope()).isEqualTo("BOOK");
        assertThat(response.pendingAction()).isNull();
        verify(pendingActionPort, never()).save(any());
    }

    @Test
    void givenPreviousMultipleBookContext_whenOrderFirstBook_thenUseFirstBookButStillRequireCheckoutInfo() {
        when(catalogBookPort.searchBooks(eq("harry potter"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder().id(101L).title("Harry Potter Tap 1").author("J.K. Rowling").build(),
                CatalogBookPort.BookDocument.builder().id(102L).title("Harry Potter Tap 2").author("J.K. Rowling").build()
        ));
        when(catalogBookPort.getBook(101L)).thenReturn(bookContext(101L, "Harry Potter Tap 1", "J.K. Rowling"));
        when(catalogBookPort.getBook(102L)).thenReturn(bookContext(102L, "Harry Potter Tap 2", "J.K. Rowling"));

        service.handleMessage(new AiAgentUseCase.AgentMessageCommand("session-context-list", 5L, "Tim Harry Potter"));
        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-context-list", 5L, "Lay cuon dau tien")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.PLACE_ORDER);
        assertThat(response.message()).doesNotContain("sách cần thao tác");
        assertThat(response.books()).hasSize(1);
        assertThat(response.books().get(0).bookId()).isEqualTo(101L);
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verifyNoInteractions(cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenPreviousMultipleBookContext_whenOrderThatBook_thenAskToChooseBookWithoutCheckoutPrompt() {
        when(catalogBookPort.searchBooks(eq("van hoc"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder().id(101L).title("Van Hoc Co Dien").author("Hector Malot").build(),
                CatalogBookPort.BookDocument.builder().id(102L).title("Van Hoc Hien Dai").author("Paulo Coelho").build()
        ));
        when(catalogBookPort.getBook(101L)).thenReturn(bookContext(101L, "Van Hoc Co Dien", "Hector Malot"));
        when(catalogBookPort.getBook(102L)).thenReturn(bookContext(102L, "Van Hoc Hien Dai", "Paulo Coelho"));

        service.handleMessage(new AiAgentUseCase.AgentMessageCommand("session-context-ambiguous", 5L, "Tim sach van hoc"));
        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-context-ambiguous", 5L, "Dat cho toi cuon do")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.PLACE_ORDER);
        assertThat(response.books()).hasSize(2);
        assertThat(response.suggestions()).hasSize(3);
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verifyNoInteractions(cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenPreviousSearchContext_whenAddFirstBookAndReferToThatBook_thenUseContextAndValidateStock() {
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder().id(10L).title("Clean Code").author("Robert C. Martin").build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(350000))
                .quantity(5)
                .isActive(true)
                .build());
        when(cartUseCase.getCartByUserId(5L)).thenReturn(cartWithItem(10L, "Clean Code", 1));

        AiAgentUseCase.AgentResponse searchResponse = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-context-clean-code", 5L, "Tim sach Clean Code")
        );
        AiAgentUseCase.AgentResponse addResponse = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-context-clean-code", 5L, "Them cuon dau tien vao gio")
        );
        AiAgentUseCase.AgentResponse orderThatBookResponse = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-context-clean-code", 5L, "Dat cuon do")
        );
        AiAgentUseCase.AgentResponse takeTwoResponse = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-context-clean-code", 5L, "Lay 2 cuon")
        );

        assertThat(searchResponse.intent()).isEqualTo(AiAgentIntent.SEARCH_BOOK);
        assertThat(addResponse.intent()).isEqualTo(AiAgentIntent.ADD_TO_CART);
        verify(cartUseCase).addItem(eq(5L), argThat(command ->
                command.getBookId().equals(10L) && command.getQuantity() == 1
        ));
        assertThat(orderThatBookResponse.intent()).isEqualTo(AiAgentIntent.PLACE_ORDER);
        assertThat(orderThatBookResponse.books()).hasSize(1);
        assertThat(orderThatBookResponse.books().get(0).bookId()).isEqualTo(10L);
        assertThat(orderThatBookResponse.confirmationCard()).isNull();
        assertThat(takeTwoResponse.intent()).isEqualTo(AiAgentIntent.PLACE_ORDER);
        assertThat(takeTwoResponse.message()).contains("Clean Code");
        assertThat(takeTwoResponse.message()).contains("COD");
        assertThat(takeTwoResponse.books()).hasSize(1);
        assertThat(takeTwoResponse.books().get(0).bookId()).isEqualTo(10L);
        assertThat(takeTwoResponse.pendingAction()).isNull();
        assertThat(takeTwoResponse.confirmationCard()).isNull();
        verify(orderUseCase, never()).checkout(anyLong(), any());
        verify(pendingActionPort, never()).save(any());
    }

    @Test
    void givenSearchWithMultipleHarryPotterBooks_whenHandleMessage_thenReturnCardsAndDoNotCreateOrder() {
        when(catalogBookPort.searchBooks(eq("harry potter"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder().id(101L).title("Harry Potter Tap 1").author("J.K. Rowling").build(),
                CatalogBookPort.BookDocument.builder().id(102L).title("Harry Potter Tap 2").author("J.K. Rowling").build()
        ));
        when(catalogBookPort.getBook(101L)).thenReturn(bookContext(101L, "Harry Potter Tap 1", "J.K. Rowling"));
        when(catalogBookPort.getBook(102L)).thenReturn(bookContext(102L, "Harry Potter Tap 2", "J.K. Rowling"));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-harry-potter", 5L, "Tim Harry Potter")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.SEARCH_BOOK);
        assertThat(response.cards()).hasSize(2);
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verifyNoInteractions(cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenOrderJavaAndMultipleBooksMatch_whenHandleMessage_thenAskUserToChooseBookWithoutPendingOrder() {
        when(catalogBookPort.searchBooks(eq("java"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder().id(201L).title("Java Core").author("Cay Horstmann").build(),
                CatalogBookPort.BookDocument.builder().id(202L).title("Effective Java").author("Joshua Bloch").build()
        ));
        when(catalogBookPort.getBook(201L)).thenReturn(bookContext(201L, "Java Core", "Cay Horstmann"));
        when(catalogBookPort.getBook(202L)).thenReturn(bookContext(202L, "Effective Java", "Joshua Bloch"));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-order-java", 5L, "Dat cho toi sach Java")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.PLACE_ORDER);
        assertThat(response.cards()).hasSize(2);
        assertThat(response.message()).contains("chọn");
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verifyNoInteractions(cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenBuyDoraemonAndMultipleBooksMatch_whenHandleMessage_thenAskUserToChooseBookWithoutPendingOrder() {
        when(catalogBookPort.searchBooks(eq("doraemon"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder().id(301L).title("Doraemon Tap 1").author("Fujiko F. Fujio").build(),
                CatalogBookPort.BookDocument.builder().id(302L).title("Doraemon Tap 2").author("Fujiko F. Fujio").build()
        ));
        when(catalogBookPort.getBook(301L)).thenReturn(bookContext(301L, "Doraemon Tap 1", "Fujiko F. Fujio"));
        when(catalogBookPort.getBook(302L)).thenReturn(bookContext(302L, "Doraemon Tap 2", "Fujiko F. Fujio"));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-buy-doraemon", 5L, "Mua quyen Doraemon")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.PLACE_ORDER);
        assertThat(response.cards()).hasSize(2);
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verifyNoInteractions(cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenBuyEnglishBooksAndMultipleBooksMatch_whenHandleMessage_thenAskUserToChooseBookWithoutPendingOrder() {
        when(catalogBookPort.searchBooks(eq("tieng anh"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder().id(401L).title("Sach Tieng Anh Grammar").author("Raymond Murphy").build(),
                CatalogBookPort.BookDocument.builder().id(402L).title("Sach Tieng Anh Vocabulary").author("Michael McCarthy").build()
        ));
        when(catalogBookPort.getBook(401L)).thenReturn(bookContext(401L, "Sach Tieng Anh Grammar", "Raymond Murphy"));
        when(catalogBookPort.getBook(402L)).thenReturn(bookContext(402L, "Sach Tieng Anh Vocabulary", "Michael McCarthy"));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-buy-english", 5L, "Toi muon mua sach tieng Anh")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.PLACE_ORDER);
        assertThat(response.cards()).hasSize(2);
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verifyNoInteractions(cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenCompletePlaceOrderRequest_whenHandleMessage_thenCreatePendingActionWithStructuredResponse() {
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(10L)
                        .title("Clean Code")
                        .author("Robert C. Martin")
                        .build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(120000))
                .quantity(10)
                .isActive(true)
                .build());
        when(pendingActionPort.save(any(AiAgentPendingAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-place-order", 5L,
                        "Đặt hàng COD sách Clean Code; địa chỉ giao hàng: 12 NVB, Phường 4, HCM; số điện thoại: 0909000000")
        );

        ArgumentCaptor<AiAgentPendingAction> captor = ArgumentCaptor.forClass(AiAgentPendingAction.class);
        verify(pendingActionPort).save(captor.capture());
        assertThat(captor.getValue().getIntent()).isEqualTo(AiAgentIntent.PLACE_ORDER);
        assertThat(response.intent()).isEqualTo(AiAgentIntent.PLACE_ORDER);
        assertThat(response.pendingAction()).isNotNull();
        assertThat(response.actions()).extracting(AiAgentUseCase.AgentAction::action)
                .contains("CONFIRM_PENDING_ACTION", "CANCEL_PENDING_ACTION");
        assertThat(response.confirmationCard()).isNotNull();
        verify(orderUseCase, never()).checkout(anyLong(), any());
    }

    @Test
    void givenPlaceOrderWithQuantityTwo_whenHandleMessage_thenPendingActionKeepsRequestedQuantity() throws Exception {
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(10L)
                        .title("Clean Code")
                        .author("Robert C. Martin")
                        .build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(350000))
                .quantity(12)
                .isActive(true)
                .build());
        when(pendingActionPort.save(any(AiAgentPendingAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-place-order-quantity", 5L,
                        "Dat hang COD 2 cuon sach Clean Code; dia chi giao hang: 12 NVB, HCM; so dien thoai: 0909000000")
        );

        ArgumentCaptor<AiAgentPendingAction> captor = ArgumentCaptor.forClass(AiAgentPendingAction.class);
        verify(pendingActionPort).save(captor.capture());
        AiAgentService.ActionPayload payload = new ObjectMapper()
                .readValue(captor.getValue().getPayload(), AiAgentService.ActionPayload.class);

        assertThat(response.intent()).isEqualTo(AiAgentIntent.PLACE_ORDER);
        assertThat(payload.quantity()).isEqualTo(2);
        assertThat(response.pendingAction().summary()).containsEntry("quantity", 2);
        assertThat(response.confirmationCard().description()).contains("2");
        verify(orderUseCase, never()).checkout(anyLong(), any());
    }

    @Test
    void givenCartCheckoutWithAddressAndPhoneOnly_whenHandleMessage_thenCreateCartCheckoutPendingAction() throws Exception {
        when(pendingActionPort.save(any(AiAgentPendingAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-cart-checkout-complete", 5L,
                        "Dat toan bo gio hang COD; dia chi giao hang: 12 nvb, An Bien, An Giang; so dien thoai: 0788912345")
        );

        ArgumentCaptor<AiAgentPendingAction> captor = ArgumentCaptor.forClass(AiAgentPendingAction.class);
        verify(pendingActionPort).save(captor.capture());
        AiAgentService.ActionPayload payload = new ObjectMapper()
                .readValue(captor.getValue().getPayload(), AiAgentService.ActionPayload.class);

        assertThat(response.intent()).isEqualTo(AiAgentIntent.PLACE_ORDER);
        assertThat(response.pendingAction()).isNotNull();
        assertThat(response.checkoutScope()).isEqualTo("CART");
        assertThat(payload.bookId()).isNull();
        assertThat(payload.bookTitle()).isNull();
        assertThat(payload.selectedBookIds()).isNull();
        assertThat(payload.paymentMethod()).isEqualTo("COD");
        assertThat(payload.shippingAddress()).isEqualTo("12 nvb, An Bien, An Giang");
        assertThat(payload.customerPhone()).isEqualTo("0788912345");
        assertThat(response.confirmationCard().description()).doesNotContain("Dat hang COD");
        assertThat(response.confirmationCard().description()).doesNotContain("null");
        verifyNoInteractions(catalogBookPort, cartUseCase, orderUseCase, paymentUseCase);
    }

    @Test
    void givenStructuredBookCheckoutAction_whenHandleMessage_thenKeepBookScopeForAddressStep() {
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(350000))
                .quantity(15)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand(
                        "session-structured-book",
                        5L,
                        "",
                        AiAgentUseCase.ClientAction.builder()
                                .action("PLACE_ORDER")
                                .bookId(10L)
                                .quantity(1)
                                .checkoutScope("BOOK")
                                .build()
                )
        );

        assertThat(response.books()).hasSize(1);
        assertThat(response.books().get(0).bookId()).isEqualTo(10L);
        assertThat(response.checkoutScope()).isEqualTo("BOOK");
        assertThat(response.pendingAction()).isNull();
        verify(pendingActionPort, never()).save(any());
    }

    @Test
    void givenStructuredCartCheckoutAction_whenHandleMessage_thenCreateCartPendingAction() throws Exception {
        when(pendingActionPort.save(any(AiAgentPendingAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand(
                        "session-structured-cart",
                        5L,
                        "",
                        AiAgentUseCase.ClientAction.builder()
                                .action("PLACE_ORDER")
                                .checkoutScope("CART")
                                .paymentMethod("COD")
                                .shippingAddress("12 NVB, HCM")
                                .customerPhone("0909000000")
                                .build()
                )
        );

        ArgumentCaptor<AiAgentPendingAction> captor = ArgumentCaptor.forClass(AiAgentPendingAction.class);
        verify(pendingActionPort).save(captor.capture());
        AiAgentService.ActionPayload payload = new ObjectMapper()
                .readValue(captor.getValue().getPayload(), AiAgentService.ActionPayload.class);

        assertThat(response.checkoutScope()).isEqualTo("CART");
        assertThat(payload.cartCheckout()).isTrue();
        assertThat(payload.bookId()).isNull();
        verifyNoInteractions(catalogBookPort, cartUseCase, orderUseCase, paymentUseCase);
    }

    @Test
    void givenGenericCheckoutWithContactButNoBook_whenHandleMessage_thenAskWhichBookAndDoNotCheckoutCart() {
        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-generic-checkout-complete", 5L,
                        "Dat hang COD; dia chi giao hang: 12 nvb, An Bien, An Giang; so dien thoai: 0788912345")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.PLACE_ORDER);
        assertThat(response.pendingAction()).isNull();
        assertThat(response.confirmationCard()).isNull();
        verifyNoInteractions(catalogBookPort, cartUseCase, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenConfirmedPlaceOrderForOneBookAlreadyHasTwoInCart_whenConfirmAction_thenCheckoutOnlyRequestedQuantity() {
        AiAgentPendingAction action = AiAgentPendingAction.create(
                "session-place-confirm-one",
                5L,
                AiAgentIntent.PLACE_ORDER,
                """
                        {"bookId":10,"bookTitle":"Clean Code","quantity":1,"couponCode":null,"paymentMethod":"COD","shippingAddress":"12 NVB, HCM","customerPhone":"0909000000","selectedBookIds":[10],"unitPrice":350000}
                        """,
                LocalDateTime.now().plusMinutes(10)
        );
        when(pendingActionPort.findById(action.getId())).thenReturn(Optional.of(action));
        when(pendingActionPort.save(any(AiAgentPendingAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(350000))
                .quantity(12)
                .isActive(true)
                .build());
        when(cartUseCase.getCartByUserId(5L)).thenReturn(cartWithItem(10L, "Clean Code", 2));
        when(orderUseCase.checkout(eq(5L), any(OrderInternalUseCase.CheckoutCommand.class))).thenReturn(
                OrderInternalUseCase.OrderResponse.builder()
                        .orderId(88L)
                        .totalAmount(BigDecimal.valueOf(350000))
                        .finalAmount(BigDecimal.valueOf(350000))
                        .fulfillmentStatus("CONFIRMED")
                        .items(List.of(OrderInternalUseCase.OrderItemResponse.builder()
                                .bookId(10L)
                                .title("Clean Code")
                                .quantity(1)
                                .priceAtPurchase(BigDecimal.valueOf(350000))
                                .build()))
                        .build()
        );

        AiAgentUseCase.AgentResponse response = service.confirmAction(action.getId(), 5L, "127.0.0.1");

        ArgumentCaptor<OrderInternalUseCase.CheckoutCommand> commandCaptor =
                ArgumentCaptor.forClass(OrderInternalUseCase.CheckoutCommand.class);
        verify(orderUseCase).checkout(eq(5L), commandCaptor.capture());
        assertThat(commandCaptor.getValue().getSelectedBookIds()).containsExactly(10L);
        assertThat(commandCaptor.getValue().getSelectedBookQuantities()).containsEntry(10L, 1);
        verify(cartUseCase, never()).addItem(eq(5L), any());
        assertThat(response.order().orderId()).isEqualTo(88L);
    }

    @Test
    void givenConfirmedPlaceOrderForBookAddedByAgent_whenConfirmAction_thenReturnFreshCartAfterCheckout() {
        AiAgentPendingAction action = AiAgentPendingAction.create(
                "session-place-confirm-fresh-cart",
                5L,
                AiAgentIntent.PLACE_ORDER,
                """
                        {"bookId":10,"bookTitle":"Clean Code","quantity":1,"couponCode":null,"paymentMethod":"COD","shippingAddress":"12 NVB, HCM","customerPhone":"0909000000","selectedBookIds":[10],"unitPrice":350000}
                        """,
                LocalDateTime.now().plusMinutes(10)
        );
        when(pendingActionPort.findById(action.getId())).thenReturn(Optional.of(action));
        when(pendingActionPort.save(any(AiAgentPendingAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(350000))
                .quantity(12)
                .isActive(true)
                .build());
        when(cartUseCase.getCartByUserId(5L)).thenReturn(
                CartInternalUseCase.CartResponse.builder()
                        .userId(5L)
                        .totalAmount(BigDecimal.ZERO)
                        .items(List.of())
                        .build()
        );
        when(orderUseCase.checkout(eq(5L), any(OrderInternalUseCase.CheckoutCommand.class))).thenReturn(
                OrderInternalUseCase.OrderResponse.builder()
                        .orderId(89L)
                        .totalAmount(BigDecimal.valueOf(350000))
                        .finalAmount(BigDecimal.valueOf(350000))
                        .fulfillmentStatus("CONFIRMED")
                        .items(List.of(OrderInternalUseCase.OrderItemResponse.builder()
                                .bookId(10L)
                                .title("Clean Code")
                                .quantity(1)
                                .priceAtPurchase(BigDecimal.valueOf(350000))
                                .build()))
                        .build()
        );

        AiAgentUseCase.AgentResponse response = service.confirmAction(action.getId(), 5L, "127.0.0.1");

        assertThat(response.order().orderId()).isEqualTo(89L);
        assertThat(response.cart()).isNotNull();
        assertThat(response.cart().items()).isEmpty();
        verify(cartUseCase).addItem(eq(5L), any(CartInternalUseCase.AddItemCommand.class));
    }

    @Test
    void givenCheckoutAddressWithoutPhone_whenHandleMessage_thenAskPhoneWithoutAddressSuggestion() {
        when(catalogBookPort.searchBooks(eq("clean code - ban thuc hanh"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(10L)
                        .title("Clean Code - Ban thuc hanh")
                        .author("Robert C. Martin")
                        .build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code - Ban thuc hanh")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(120000))
                .quantity(10)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-checkout-phone", 5L,
                        "Đặt hàng COD sách Clean Code - Ban thuc hanh; địa chỉ giao hàng: 12 NVB, Phuong 4, HCM")
        );

        assertThat(response.message()).contains("số điện thoại");
        assertThat(response.suggestions()).contains("Nhập số điện thoại");
        assertThat(response.suggestions()).doesNotContain("Chọn địa chỉ giao hàng");
        assertThat(response.confirmationCard()).isNull();
        verify(pendingActionPort, never()).save(any());
    }

    @Test
    void givenClearAddToCartRequest_whenHandleMessage_thenCreatePendingActionWithoutLlm() {
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(10L)
                        .title("Clean Code")
                        .author("Robert C. Martin")
                        .build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(120000))
                .quantity(10)
                .isActive(true)
                .build());
        when(cartUseCase.getCartByUserId(5L)).thenReturn(cartWithItem(10L, "Clean Code", 1));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-cart-fast", 5L, "thêm 1 cuốn clean code vào giỏ")
        );

        assertThat(response.confirmationCard()).isNull();
        assertThat(response.message()).contains("Clean Code");
        verifyNoInteractions(llmPort);

        verify(cartUseCase).addItem(eq(5L), any(CartInternalUseCase.AddItemCommand.class));
        verify(pendingActionPort, never()).save(any());
    }

    @Test
    void givenUpdateCartQuantityRequest_whenHandleMessage_thenUpdateCurrentUserCartOnly() {
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(10L)
                        .title("Clean Code")
                        .author("Robert C. Martin")
                        .build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(350000))
                .quantity(10)
                .isActive(true)
                .build());
        when(cartUseCase.getCartByUserId(5L)).thenReturn(cartWithItem(10L, "Clean Code", 2));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-cart-update", 5L, "Doi so luong Clean Code trong gio thanh 2 cuon")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.UPDATE_CART_QUANTITY);
        verify(cartUseCase).updateItemQuantity(eq(5L), argThat(command ->
                command.getBookId().equals(10L) && command.getQuantity() == 2
        ));
        verify(cartUseCase).getCartByUserId(5L);
        verifyNoInteractions(llmPort, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenRemoveCartRequest_whenHandleMessage_thenRemoveFromCurrentUserCartOnly() {
        when(catalogBookPort.searchBooks(eq("atomic habits"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(20L)
                        .title("Atomic Habits")
                        .author("James Clear")
                        .build()
        ));
        when(catalogBookPort.getBook(20L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(20L)
                .title("Atomic Habits")
                .author("James Clear")
                .price(BigDecimal.valueOf(189000))
                .quantity(10)
                .isActive(true)
                .build());
        when(cartUseCase.getCartByUserId(5L)).thenReturn(cartWithItem(20L, "Atomic Habits", 1));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-cart-remove", 5L, "Xoa Atomic Habits khoi gio hang")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.REMOVE_FROM_CART);
        verify(cartUseCase).removeItem(5L, 20L);
        verify(cartUseCase).getCartByUserId(5L);
        verifyNoInteractions(llmPort, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenRemoveCartRequestWithBoRaKhoi_whenHandleMessage_thenRemoveFromCurrentUserCartOnly() {
        when(catalogBookPort.searchBooks(eq("dac nhan tam"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(30L)
                        .title("Dac Nhan Tam")
                        .author("Dale Carnegie")
                        .build()
        ));
        when(catalogBookPort.getBook(30L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(30L)
                .title("Dac Nhan Tam")
                .author("Dale Carnegie")
                .price(BigDecimal.valueOf(99000))
                .quantity(10)
                .isActive(true)
                .build());
        when(cartUseCase.getCartByUserId(5L)).thenReturn(cartWithItem(30L, "Dac Nhan Tam", 1));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-cart-remove-bo", 5L, "Bo quyen Dac nhan tam ra khoi gio")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.REMOVE_FROM_CART);
        verify(cartUseCase).removeItem(5L, 30L);
        verify(cartUseCase).getCartByUserId(5L);
        verifyNoInteractions(llmPort, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenViewCartRequest_whenHandleMessage_thenReturnCurrentUserCart() {
        when(cartUseCase.getCartByUserId(5L)).thenReturn(cartWithItem(10L, "Clean Code", 1));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-cart-view", 5L, "Cho toi xem gio hang hien tai")
        );

        assertThat(response.intent()).isEqualTo(AiAgentIntent.VIEW_CART);
        assertThat(response.cart()).isNotNull();
        assertThat(response.cart().items()).hasSize(1);
        verify(cartUseCase).getCartByUserId(5L);
        verifyNoInteractions(llmPort, orderUseCase, paymentUseCase, pendingActionPort);
    }

    @Test
    void givenLatestOrderRequest_whenHandleMessage_thenUseVietnameseStatusLabel() {
        when(orderUseCase.getMyOrders(5L)).thenReturn(List.of(
                OrderInternalUseCase.OrderResponse.builder()
                        .orderId(63L)
                        .updatedAt(LocalDateTime.now())
                        .fulfillmentStatus("CONFIRMED")
                        .build()
        ));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-latest-order-status", 5L, "Don hang gan nhat cua toi")
        );

        assertThat(response.message()).contains("Chờ xác nhận");
        assertThat(response.message()).doesNotContain("CONFIRMED");
        assertThat(response.order().fulfillmentStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void givenCancelOrderWithOrderCode_whenHandleMessage_thenCreatePendingActionWithoutCancellingImmediately() throws Exception {
        when(pendingActionPort.save(any(AiAgentPendingAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-cancel-order-code", 5L, "Huy don hang ORD001 giup toi")
        );

        ArgumentCaptor<AiAgentPendingAction> captor = ArgumentCaptor.forClass(AiAgentPendingAction.class);
        verify(pendingActionPort).save(captor.capture());
        AiAgentService.ActionPayload payload = new ObjectMapper()
                .readValue(captor.getValue().getPayload(), AiAgentService.ActionPayload.class);

        assertThat(response.intent()).isEqualTo(AiAgentIntent.CANCEL_ORDER);
        assertThat(response.pendingAction()).isNotNull();
        assertThat(response.confirmationCard()).isNotNull();
        assertThat(response.confirmationCard().title()).isEqualTo("Xác nhận hủy đơn hàng");
        assertThat(response.confirmationCard().description()).isEqualTo("Hủy đơn hàng #1.");
        assertThat(payload.orderId()).isEqualTo(1L);
        verify(orderUseCase, never()).cancelMyPendingOrder(anyLong(), anyLong(), anyString());
    }

    @Test
    void givenCancelLatestOrderRequest_whenHandleMessage_thenResolveLatestOrderAndCreatePendingAction() throws Exception {
        when(pendingActionPort.save(any(AiAgentPendingAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderUseCase.getMyOrders(5L)).thenReturn(List.of(
                OrderInternalUseCase.OrderResponse.builder()
                        .orderId(10L)
                        .updatedAt(LocalDateTime.now().minusHours(2))
                        .fulfillmentStatus("PENDING")
                        .build(),
                OrderInternalUseCase.OrderResponse.builder()
                        .orderId(12L)
                        .updatedAt(LocalDateTime.now().minusMinutes(3))
                        .fulfillmentStatus("CONFIRMED")
                        .build()
        ));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand("session-cancel-latest", 5L, "Toi muon huy don gan nhat")
        );

        ArgumentCaptor<AiAgentPendingAction> captor = ArgumentCaptor.forClass(AiAgentPendingAction.class);
        verify(pendingActionPort).save(captor.capture());
        AiAgentService.ActionPayload payload = new ObjectMapper()
                .readValue(captor.getValue().getPayload(), AiAgentService.ActionPayload.class);

        assertThat(response.intent()).isEqualTo(AiAgentIntent.CANCEL_ORDER);
        assertThat(response.pendingAction()).isNotNull();
        assertThat(payload.orderId()).isEqualTo(12L);
        verify(orderUseCase).getMyOrders(5L);
        verify(orderUseCase, never()).cancelMyPendingOrder(anyLong(), anyLong(), anyString());
    }

    @Test
    void givenChangePaymentMethodToCodRequest_whenHandleMessage_thenCreatePendingAction() throws Exception {
        when(pendingActionPort.save(any(AiAgentPendingAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiAgentUseCase.AgentResponse response = service.handleMessage(
                new AiAgentUseCase.AgentMessageCommand(
                        "session-change-payment-method", 5L,
                        "Doi phuong thuc thanh toan don hang ORD077 sang COD")
        );

        ArgumentCaptor<AiAgentPendingAction> captor = ArgumentCaptor.forClass(AiAgentPendingAction.class);
        verify(pendingActionPort).save(captor.capture());
        AiAgentService.ActionPayload payload = new ObjectMapper()
                .readValue(captor.getValue().getPayload(), AiAgentService.ActionPayload.class);

        assertThat(response.intent()).isEqualTo(AiAgentIntent.CHANGE_PAYMENT_METHOD);
        assertThat(response.confirmationCard().description()).contains("#77").contains("COD");
        assertThat(payload.orderId()).isEqualTo(77L);
        assertThat(payload.paymentMethod()).isEqualTo("COD");
        verifyNoInteractions(paymentUseCase);
    }

    @Test
    void givenConfirmedChangePaymentMethodToCodAction_whenConfirmAction_thenSwitchPaymentMethod() {
        AiAgentPendingAction action = AiAgentPendingAction.create(
                "session-confirm-change-payment", 5L, AiAgentIntent.CHANGE_PAYMENT_METHOD,
                """
                        {"quantity":1,"paymentMethod":"COD","orderId":77}
                        """,
                LocalDateTime.now().plusMinutes(10)
        );
        when(pendingActionPort.findById(action.getId())).thenReturn(Optional.of(action));
        when(pendingActionPort.save(any(AiAgentPendingAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderUseCase.getMyOrderById(77L, 5L)).thenReturn(
                OrderInternalUseCase.OrderResponse.builder()
                        .orderId(77L)
                        .fulfillmentStatus("CONFIRMED")
                        .build()
        );

        AiAgentUseCase.AgentResponse response = service.confirmAction(action.getId(), 5L, "127.0.0.1");

        assertThat(response.intent()).isEqualTo(AiAgentIntent.CHANGE_PAYMENT_METHOD);
        assertThat(response.message()).contains("#77").contains("COD");
        verify(paymentUseCase).switchPendingVnpayOrderToCod(77L, 5L);
    }

    @Test
    void givenStockContext_whenPlaceTwoCopies_thenPendingActionTargetsThatBookWithRequestedQuantity() throws Exception {
        when(pendingActionPort.save(any(AiAgentPendingAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(10L)
                        .title("Clean Code")
                        .author("Robert C. Martin")
                        .build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(350000))
                .quantity(10)
                .isActive(true)
                .build());

        service.handleMessage(new AiAgentUseCase.AgentMessageCommand(
                "session-context-order-quantity", 5L, "Sach Clean Code con hang khong?"
        ));
        AiAgentUseCase.AgentResponse response = service.handleMessage(new AiAgentUseCase.AgentMessageCommand(
                "session-context-order-quantity", 5L,
                "Dat 2 cuon; dia chi giao hang: 12 NVB, HCM; so dien thoai: 0909000000; thanh toan COD"
        ));

        ArgumentCaptor<AiAgentPendingAction> captor = ArgumentCaptor.forClass(AiAgentPendingAction.class);
        verify(pendingActionPort).save(captor.capture());
        AiAgentService.ActionPayload payload = new ObjectMapper()
                .readValue(captor.getValue().getPayload(), AiAgentService.ActionPayload.class);

        assertThat(response.intent()).isEqualTo(AiAgentIntent.PLACE_ORDER);
        assertThat(payload.bookId()).isEqualTo(10L);
        assertThat(payload.selectedBookIds()).containsExactly(10L);
        assertThat(payload.quantity()).isEqualTo(2);
        assertThat(payload.cartCheckout()).isFalse();
        verify(orderUseCase, never()).checkout(anyLong(), any());
    }

    @Test
    void givenPlaceTwoCopiesMissingCheckoutInfo_whenHandleMessage_thenExposeRequestedQuantityForAddressStep() {
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(10L)
                        .title("Clean Code")
                        .author("Robert C. Martin")
                        .build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(350000))
                .quantity(10)
                .isActive(true)
                .build());

        AiAgentUseCase.AgentResponse response = service.handleMessage(new AiAgentUseCase.AgentMessageCommand(
                "session-missing-address-quantity", 5L, "Dat 2 cuon Clean Code"
        ));

        assertThat(response.confirmationCard()).isNull();
        assertThat(response.books()).hasSize(1);
        assertThat(response.books().get(0).quantity()).isEqualTo(10);
        assertThat(response.books().get(0).requestedQuantity()).isEqualTo(2);
        verify(pendingActionPort, never()).save(any());
    }

    @Test
    void givenPlaceTwoCopiesMissingCheckoutInfo_whenProvideAddressWithoutQuantity_thenKeepDraftQuantity() throws Exception {
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(10L)
                        .title("Clean Code")
                        .author("Robert C. Martin")
                        .build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(350000))
                .quantity(10)
                .isActive(true)
                .build());
        when(pendingActionPort.save(any(AiAgentPendingAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handleMessage(new AiAgentUseCase.AgentMessageCommand(
                "session-checkout-draft-quantity", 5L, "Dat 2 cuon Clean Code"
        ));
        AiAgentUseCase.AgentResponse response = service.handleMessage(new AiAgentUseCase.AgentMessageCommand(
                "session-checkout-draft-quantity", 5L,
                "Dat hang COD sach Clean Code; dia chi giao hang: 12 NVB, HCM; so dien thoai: 0909000000"
        ));

        ArgumentCaptor<AiAgentPendingAction> captor = ArgumentCaptor.forClass(AiAgentPendingAction.class);
        verify(pendingActionPort).save(captor.capture());
        AiAgentService.ActionPayload payload = new ObjectMapper()
                .readValue(captor.getValue().getPayload(), AiAgentService.ActionPayload.class);

        assertThat(payload.quantity()).isEqualTo(2);
        assertThat(response.confirmationCard().description()).contains("2");
    }

    @Test
    void givenCheckoutDraftFromAnotherInstance_whenProvideAddressWithoutQuantity_thenKeepRedisDraftQuantity() throws Exception {
        when(checkoutDraftPort.findBySessionId("session-redis-checkout-draft")).thenReturn(Optional.of(
                new AiCheckoutDraftPersistencePort.CheckoutDraft(10L, "Clean Code", 2)
        ));
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(10L)
                        .title("Clean Code")
                        .author("Robert C. Martin")
                        .build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(350000))
                .quantity(10)
                .isActive(true)
                .build());
        when(pendingActionPort.save(any(AiAgentPendingAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiAgentUseCase.AgentResponse response = service.handleMessage(new AiAgentUseCase.AgentMessageCommand(
                "session-redis-checkout-draft", 5L,
                "Dat hang COD sach Clean Code; dia chi giao hang: 12 NVB, HCM; so dien thoai: 0909000000"
        ));

        ArgumentCaptor<AiAgentPendingAction> captor = ArgumentCaptor.forClass(AiAgentPendingAction.class);
        verify(pendingActionPort).save(captor.capture());
        AiAgentService.ActionPayload payload = new ObjectMapper()
                .readValue(captor.getValue().getPayload(), AiAgentService.ActionPayload.class);

        assertThat(payload.quantity()).isEqualTo(2);
        assertThat(response.confirmationCard().description()).contains("2");
        verify(checkoutDraftPort).delete("session-redis-checkout-draft");
    }

    @Test
    void givenStockContext_whenPlaceOneCopyWithVnpay_thenPendingActionDoesNotCheckoutWholeCart() throws Exception {
        when(pendingActionPort.save(any(AiAgentPendingAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(catalogBookPort.searchBooks(eq("clean code"), isNull())).thenReturn(List.of(
                CatalogBookPort.BookDocument.builder()
                        .id(10L)
                        .title("Clean Code")
                        .author("Robert C. Martin")
                        .build()
        ));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(350000))
                .quantity(10)
                .isActive(true)
                .build());

        service.handleMessage(new AiAgentUseCase.AgentMessageCommand(
                "session-context-vnpay", 5L, "Sach Clean Code con hang khong?"
        ));
        service.handleMessage(new AiAgentUseCase.AgentMessageCommand(
                "session-context-vnpay", 5L,
                "Dat 1 cuon va chon thanh toan vnpay; dia chi giao hang: 12 NVB, HCM; so dien thoai: 0909000000"
        ));

        ArgumentCaptor<AiAgentPendingAction> captor = ArgumentCaptor.forClass(AiAgentPendingAction.class);
        verify(pendingActionPort).save(captor.capture());
        AiAgentService.ActionPayload payload = new ObjectMapper()
                .readValue(captor.getValue().getPayload(), AiAgentService.ActionPayload.class);

        assertThat(payload.bookId()).isEqualTo(10L);
        assertThat(payload.quantity()).isEqualTo(1);
        assertThat(payload.paymentMethod()).isEqualTo("VNPAY");
        assertThat(payload.selectedBookIds()).containsExactly(10L);
        assertThat(payload.cartCheckout()).isFalse();
        verify(orderUseCase, never()).checkout(anyLong(), any());
    }

    @Test
    void givenConfirmedVnpayCheckoutAction_whenConfirmAction_thenCheckoutAndReturnPaymentUrl() {
        AiAgentPendingAction action = AiAgentPendingAction.create(
                "session-4",
                5L,
                AiAgentIntent.CHECKOUT,
                """
                        {"bookId":10,"quantity":1,"couponCode":null,"paymentMethod":"VNPAY","shippingAddress":"12 Nguyễn Văn Bảo","customerPhone":"0909000000","selectedBookIds":[10]}
                        """,
                LocalDateTime.now().plusMinutes(10)
        );
        when(pendingActionPort.findById(action.getId())).thenReturn(Optional.of(action));
        when(pendingActionPort.save(any(AiAgentPendingAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(catalogBookPort.getBook(10L)).thenReturn(CatalogBookPort.BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .price(BigDecimal.valueOf(120000))
                .quantity(10)
                .isActive(true)
                .build());
        when(orderUseCase.checkout(eq(5L), any(OrderInternalUseCase.CheckoutCommand.class))).thenReturn(
                OrderInternalUseCase.OrderResponse.builder()
                        .orderId(77L)
                        .totalAmount(BigDecimal.valueOf(120000))
                        .finalAmount(BigDecimal.valueOf(120000))
                        .fulfillmentStatus("PENDING")
                        .build()
        );
        when(paymentUseCase.createPaymentUrl(77L, 5L, "127.0.0.1")).thenReturn("https://pay.test/77");

        AiAgentUseCase.AgentResponse response = service.confirmAction(action.getId(), 5L, "127.0.0.1");

        assertThat(response.order().orderId()).isEqualTo(77L);
        assertThat(response.redirectUrl()).isEqualTo("https://pay.test/77");
        assertThat(action.getConfirmedAt()).isNotNull();
        verify(orderUseCase).checkout(eq(5L), any(OrderInternalUseCase.CheckoutCommand.class));
        verify(paymentUseCase).createPaymentUrl(77L, 5L, "127.0.0.1");
    }

    private CartInternalUseCase.CartResponse cartWithItem(Long bookId, String title, int quantity) {
        return CartInternalUseCase.CartResponse.builder()
                .userId(5L)
                .totalAmount(BigDecimal.valueOf(120000))
                .items(List.of(CartInternalUseCase.CartItemResponse.builder()
                        .bookId(bookId)
                        .title(title)
                        .price(BigDecimal.valueOf(120000))
                        .quantity(quantity)
                        .build()))
                .build();
    }

    private CatalogBookPort.BookContext book(Long id) {
        return CatalogBookPort.BookContext.builder()
                .id(id)
                .title("Đắc Nhân Tâm")
                .author("Dale Carnegie")
                .price(BigDecimal.valueOf(99000))
                .quantity(10)
                .isActive(true)
                .build();
    }
    private CatalogBookPort.BookContext bookContext(Long id, String title, String author) {
        return CatalogBookPort.BookContext.builder()
                .id(id)
                .title(title)
                .author(author)
                .price(BigDecimal.valueOf(120000))
                .quantity(10)
                .isActive(true)
                .build();
    }
}
