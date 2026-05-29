package iuh.fit.se.modules.ai.application.service;

import iuh.fit.se.modules.ai.application.port.out.CatalogBookPort;
import iuh.fit.se.modules.ai.application.port.out.CatalogBookPort.BookContext;
import iuh.fit.se.modules.ai.application.port.out.SalesRankingPort;
import iuh.fit.se.modules.ai.application.port.out.VectorStorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiSearchServiceTest {

    @Mock
    private VectorStorePort vectorStorePort;

    @Mock
    private CatalogBookPort catalogBookPort;

    @Mock
    private SalesRankingPort salesRankingPort;

    private AiSearchService aiSearchService;

    @BeforeEach
    void setUp() {
        aiSearchService = new AiSearchService(vectorStorePort, catalogBookPort, salesRankingPort);
    }

    @Test
    void givenExplicitTopicAndInsufficientMatches_whenSearchSemantic_thenReturnOnlyMatchingBooks() {
        String query = "goi y 3 sach trinh tham";
        when(vectorStorePort.findSimilarBooks(query, 8)).thenReturn(List.of(10L, 11L, 12L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("An mang tren song Nile")
                .author("Agatha Christie")
                .description("Tieu thuyet dieu tra kinh dien.")
                .keywords(Set.of("dieu tra"))
                .categoryNames(Set.of("Trinh tham"))
                .quantity(1)
                .isActive(true)
                .build());
        when(catalogBookPort.getBook(11L)).thenReturn(BookContext.builder()
                .id(11L)
                .title("Sach lap trinh Java")
                .author("SEBook")
                .description("Sach cong nghe thong tin.")
                .quantity(4)
                .isActive(true)
                .build());
        when(catalogBookPort.getBook(12L)).thenReturn(BookContext.builder()
                .id(12L)
                .title("Ky nang song")
                .author("SEBook")
                .description("Sach phat trien ban than.")
                .quantity(5)
                .isActive(true)
                .build());

        List<Long> result = aiSearchService.searchSemantic(query, 8);

        assertEquals(List.of(10L), result);
    }

    @Test
    void givenLiteratureTopic_whenSearchSemantic_thenExcludeChildrenCategoryBooks() {
        String query = "goi y 3 sach van hoc";
        when(vectorStorePort.findSimilarBooks(query, 8)).thenReturn(List.of(10L, 11L, 12L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Tat den")
                .author("Ngo Tat To")
                .categoryNames(Set.of("Van hoc"))
                .quantity(2)
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
                .title("Nguoi dieu khien me cung")
                .author("Donato Carrisi")
                .categoryNames(Set.of("Van hoc"))
                .quantity(26)
                .isActive(true)
                .build());
        when(catalogBookPort.searchBooksByCategoryName("van hoc")).thenReturn(List.of(
                BookContext.builder()
                        .id(10L)
                        .title("Tat den")
                        .author("Ngo Tat To")
                        .categoryNames(Set.of("Van hoc"))
                        .quantity(2)
                        .isActive(true)
                        .build(),
                BookContext.builder()
                        .id(12L)
                        .title("Nguoi dieu khien me cung")
                        .author("Donato Carrisi")
                        .categoryNames(Set.of("Van hoc"))
                        .quantity(26)
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

        List<Long> result = aiSearchService.searchSemantic(query, 8);

        assertEquals(List.of(10L, 12L, 13L), result);
    }

    @Test
    void givenTitleSearchIntentWithCategoryWord_whenSearchSemantic_thenFilterByTitleNotCategory() {
        String query = "tim sach co chu thieu nhi trong ten";
        when(vectorStorePort.findSimilarBooks(query, 8)).thenReturn(List.of(10L, 11L, 12L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Van hoc thieu nhi Viet Nam")
                .author("Tac gia A")
                .categoryNames(Set.of("Van hoc"))
                .quantity(2)
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
                .title("Sach lap trinh cho tre")
                .author("Tac gia B")
                .categoryNames(Set.of("Thieu nhi"))
                .quantity(5)
                .isActive(true)
                .build());

        List<Long> result = aiSearchService.searchSemantic(query, 8);

        assertEquals(List.of(10L), result);
    }

    @Test
    void givenTitleSearchIntentWithUnknownTerm_whenSearchSemantic_thenFilterByExtractedTitleTerm() {
        String query = "3 sach co chu vo thuat trong ten";
        when(vectorStorePort.findSimilarBooks(query, 8)).thenReturn(List.of(10L, 11L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Vo thuat thieu lam")
                .author("Tac gia A")
                .categoryNames(Set.of("The thao"))
                .quantity(3)
                .isActive(true)
                .build());
        when(catalogBookPort.getBook(11L)).thenReturn(BookContext.builder()
                .id(11L)
                .title("Sach suc khoe")
                .author("Tac gia B")
                .categoryNames(Set.of("Suc khoe"))
                .quantity(3)
                .isActive(true)
                .build());

        List<Long> result = aiSearchService.searchSemantic(query, 8);

        assertEquals(List.of(10L), result);
    }

    @Test
    void givenCookingTopicWithUnrelatedSemanticMatch_whenSearchSemantic_thenReturnNoBooks() {
        String query = "tim sach nau an";
        when(vectorStorePort.findSimilarBooks(query, 8)).thenReturn(List.of(10L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Sach lap trinh Java")
                .author("Tac gia A")
                .categoryNames(Set.of("Lap trinh"))
                .quantity(3)
                .isActive(true)
                .build());

        List<Long> result = aiSearchService.searchSemantic(query, 8);

        assertEquals(List.of(), result);
    }

    @Test
    void givenHorrorTopicWithUnrelatedSemanticMatch_whenSearchSemantic_thenReturnNoBooks() {
        String query = "goi y 3 sach kinh di";
        when(vectorStorePort.findSimilarBooks(query, 8)).thenReturn(List.of(10L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Frankenstein")
                .author("Mary Shelley")
                .categoryNames(Set.of("Van hoc"))
                .quantity(7)
                .isActive(true)
                .build());
        when(catalogBookPort.searchBooksByCategoryName("kinh di")).thenReturn(List.of());

        List<Long> result = aiSearchService.searchSemantic(query, 8);

        assertEquals(List.of(), result);
    }

    @Test
    void givenVagueCatalogKeywordWithUnrelatedSemanticMatches_whenSearchSemantic_thenReturnNoBooks() {
        String query = "sach ma";
        when(vectorStorePort.findSimilarBooks(query, 8)).thenReturn(List.of(10L, 11L));
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

        List<Long> result = aiSearchService.searchSemantic(query, 8);

        assertEquals(List.of(), result);
    }

    @Test
    void givenHotBooksIntent_whenSearchSemantic_thenReturnSalesRankingBooks() {
        String query = "goi y 3 sach hot";
        when(salesRankingPort.getTopSellingBooks(3)).thenReturn(List.of(
                BookContext.builder().id(10L).title("Top 1").quantity(3).isActive(true).build(),
                BookContext.builder().id(11L).title("Top 2").quantity(4).isActive(true).build(),
                BookContext.builder().id(12L).title("Top 3").quantity(5).isActive(true).build()
        ));

        List<Long> result = aiSearchService.searchSemantic(query, 8);

        assertEquals(List.of(10L, 11L, 12L), result);
    }

    @Test
    void givenAudienceIntent_whenSearchSemantic_thenFilterByAudienceKeyword() {
        String query = "goi y 2 sach danh cho giao vien";
        when(vectorStorePort.findSimilarBooks(query, 8)).thenReturn(List.of(10L, 11L));
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

        List<Long> result = aiSearchService.searchSemantic(query, 8);

        assertEquals(List.of(10L), result);
    }

    @Test
    void givenJobAudienceIntentWithOnlyLooseDescriptionMatch_whenSearchSemantic_thenReturnNoBooks() {
        String query = "sach danh cho bac si";
        when(vectorStorePort.findSimilarBooks(query, 8)).thenReturn(List.of(10L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Nguoi dieu khien me cung")
                .author("Donato Carrisi")
                .description("Cau chuyen dieu tra co nhan vat bac si va mot benh vien bo hoang.")
                .categoryNames(Set.of("Trinh tham"))
                .quantity(26)
                .isActive(true)
                .build());

        List<Long> result = aiSearchService.searchSemantic(query, 8);

        assertEquals(List.of(), result);
    }

    @Test
    void givenJobAudienceIntent_whenSearchSemantic_thenExpandJobKeywords() {
        String query = "goi y sach danh cho dau bep";
        when(vectorStorePort.findSimilarBooks(query, 8)).thenReturn(List.of(10L, 11L));
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

        List<Long> result = aiSearchService.searchSemantic(query, 8);

        assertEquals(List.of(10L), result);
    }

    @Test
    void givenAudienceIntentWithoutMatch_whenSearchSemantic_thenReturnNoBooks() {
        String query = "sach danh cho dau bep";
        when(vectorStorePort.findSimilarBooks(query, 8)).thenReturn(List.of(10L));
        when(catalogBookPort.getBook(10L)).thenReturn(BookContext.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .categoryNames(Set.of("Lap trinh"))
                .quantity(40)
                .isActive(true)
                .build());

        List<Long> result = aiSearchService.searchSemantic(query, 8);

        assertEquals(List.of(), result);
    }

    @Test
    void givenAuthorIntent_whenSearchSemantic_thenFilterByAuthor() {
        String query = "sach cua tac gia Mary Shelley";
        when(vectorStorePort.findSimilarBooks(query, 8)).thenReturn(List.of(10L, 11L));
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

        List<Long> result = aiSearchService.searchSemantic(query, 8);

        assertEquals(List.of(10L), result);
    }
}
