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

    record CreateBookCommand(String title, String author, String description, BigDecimal price, int quantity, byte[] imageFile, List<Long> categoryIds) {}

    record UpdateBookCommand(String title, String author, String description, BigDecimal price, int quantity, byte[] imageFile, List<Long> categoryIds) {}
}
