package iuh.fit.se.modules.catalog.application.port.in;


import java.math.BigDecimal;
import java.util.List;

/**
 * BookUseCase — Inbound Port (Public/Internal API).
 */
public interface BookUseCase {

    BookDTO createBook(CreateBookCommand command);

    BookDTO updateBook(Long id, UpdateBookCommand command);

    void deleteBook(Long id);

    BookDTO getBook(Long id);

    List<BookDTO> searchBooks(String title, Long categoryId);

    // Dùng cho module Order/Inventory
    void updateStock(Long id, int amount, boolean isIncrease);
    
    void syncStock(Long id, int quantity);

    record CreateBookCommand(
            String title, String author, String description,
            BigDecimal price, BigDecimal originalPrice,
            int quantity, byte[] imageFile, List<Long> categoryIds,
            // Metadata
            String publisher, String isbn, Integer publicationYear,
            String language, List<String> keywords,
            // Physical specs
            Integer pageCount, String coverType,
            Integer weight, Integer length, Integer width, Integer height
    ) {}

    record UpdateBookCommand(
            String title, String author, String description,
            BigDecimal price, BigDecimal originalPrice,
            Integer quantity, byte[] imageFile, List<Long> categoryIds,
            // Metadata (tất cả nullable — null = giữ nguyên giá trị cũ)
            String publisher, String isbn, Integer publicationYear,
            String language, List<String> keywords,
            // Physical specs
            Integer pageCount, String coverType,
            Integer weight, Integer length, Integer width, Integer height
    ) {}
}
