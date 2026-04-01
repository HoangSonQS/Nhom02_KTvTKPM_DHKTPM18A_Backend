package iuh.fit.se.modules.catalog.application.port.in;

import iuh.fit.se.modules.catalog.domain.Book;
import java.math.BigDecimal;
import java.util.List;

/**
 * BookUseCase — Inbound Port (Public/Internal API).
 */
public interface BookUseCase {

    Book createBook(CreateBookCommand command);

    Book updateBook(Long id, UpdateBookCommand command);

    void deleteBook(Long id);

    Book getBook(Long id);

    List<Book> searchBooks(String title, Long categoryId);

    // Dùng cho module Order/Inventory
    void updateStock(Long id, int amount, boolean isIncrease);

    record CreateBookCommand(String title, String author, String description, BigDecimal price, int quantity, byte[] imageFile, List<Long> categoryIds) {}

    record UpdateBookCommand(String title, String author, String description, BigDecimal price, int quantity, byte[] imageFile, List<Long> categoryIds) {}
}
