package iuh.fit.se.modules.catalog.adapter.inbound.web;

import iuh.fit.se.modules.catalog.application.port.in.BookDTO;
import iuh.fit.se.modules.catalog.application.port.in.BookUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * BookController — Inbound Adapter cho Book.
 */
@RestController
@RequestMapping("/api/v1/catalog/books")
@RequiredArgsConstructor
public class BookController {

    private final BookUseCase bookUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookDTO>>> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success(bookUseCase.searchBooks(title, categoryId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookDTO>> getBook(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bookUseCase.getBook(id)));
    }

    @PreAuthorize("hasAuthority('CATALOG_BOOK_CREATE')")
    @PostMapping
    public ResponseEntity<ApiResponse<BookDTO>> create(
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam(value = "originalPrice", required = false) BigDecimal originalPrice,
            @RequestParam("quantity") int quantity,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam("categoryIds") List<Long> categoryIds,
            // Metadata
            @RequestParam(value = "publisher", required = false) String publisher,
            @RequestParam(value = "isbn", required = false) String isbn,
            @RequestParam(value = "publicationYear", required = false) Integer publicationYear,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "keywords", required = false) List<String> keywords,
            // Physical specs
            @RequestParam(value = "pageCount", required = false) Integer pageCount,
            @RequestParam(value = "coverType", required = false) String coverType,
            @RequestParam(value = "weight", required = false) Integer weight,
            @RequestParam(value = "length", required = false) Integer length,
            @RequestParam(value = "width", required = false) Integer width,
            @RequestParam(value = "height", required = false) Integer height
    ) throws IOException {

        byte[] imageBytes = (image != null && !image.isEmpty()) ? image.getBytes() : null;

        BookDTO book = bookUseCase.createBook(new BookUseCase.CreateBookCommand(
                title, author, description, price, originalPrice, quantity, imageBytes, categoryIds,
                publisher, isbn, publicationYear, language, keywords,
                pageCount, coverType, weight, length, width, height));

        return ResponseEntity.ok(ApiResponse.success("Tạo sách thành công", book));
    }

    @PreAuthorize("hasAuthority('CATALOG_BOOK_UPDATE')")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<BookDTO>> update(
            @PathVariable Long id,
            // Tất cả fields đều optional — chỉ gửi những gì cần thay đổi (Partial Update)
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "author", required = false) String author,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "price", required = false) BigDecimal price,
            @RequestParam(value = "originalPrice", required = false) BigDecimal originalPrice,
            @RequestParam(value = "quantity", required = false) Integer quantity,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
            // Metadata
            @RequestParam(value = "publisher", required = false) String publisher,
            @RequestParam(value = "isbn", required = false) String isbn,
            @RequestParam(value = "publicationYear", required = false) Integer publicationYear,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "keywords", required = false) List<String> keywords,
            // Physical specs
            @RequestParam(value = "pageCount", required = false) Integer pageCount,
            @RequestParam(value = "coverType", required = false) String coverType,
            @RequestParam(value = "weight", required = false) Integer weight,
            @RequestParam(value = "length", required = false) Integer length,
            @RequestParam(value = "width", required = false) Integer width,
            @RequestParam(value = "height", required = false) Integer height
    ) throws IOException {

        byte[] imageBytes = (image != null && !image.isEmpty()) ? image.getBytes() : null;

        BookDTO book = bookUseCase.updateBook(id, new BookUseCase.UpdateBookCommand(
                title, author, description, price, originalPrice, quantity, imageBytes, categoryIds,
                publisher, isbn, publicationYear, language, keywords,
                pageCount, coverType, weight, length, width, height));

        return ResponseEntity.ok(ApiResponse.success("Cập nhật sách thành công", book));
    }

    @PreAuthorize("hasAuthority('CATALOG_BOOK_DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        bookUseCase.deleteBook(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa sách thành công", null));
    }
}
