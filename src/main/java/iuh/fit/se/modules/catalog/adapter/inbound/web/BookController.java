package iuh.fit.se.modules.catalog.adapter.inbound.web;

import iuh.fit.se.modules.catalog.application.port.in.BookUseCase;
import iuh.fit.se.modules.catalog.domain.Book;
import iuh.fit.se.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * BookController — Inbound Adapter cho Book.
 */
@RestController
@RequestMapping("/api/catalog/books")
@RequiredArgsConstructor
public class BookController {

    private final BookUseCase bookUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Book>>> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success(bookUseCase.searchBooks(title, categoryId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Book>> getBook(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bookUseCase.getBook(id)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<Book>> create(
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("quantity") int quantity,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam("categoryIds") List<Long> categoryIds) throws IOException {

        byte[] imageBytes = (image != null && !image.isEmpty()) ? image.getBytes() : null;
        
        Book book = bookUseCase.createBook(new BookUseCase.CreateBookCommand(
                title, author, description, price, quantity, imageBytes, categoryIds));
        
        return ResponseEntity.ok(ApiResponse.success("Tạo sách thành công", book));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Book>> update(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("quantity") int quantity,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam("categoryIds") List<Long> categoryIds) throws IOException {

        byte[] imageBytes = (image != null && !image.isEmpty()) ? image.getBytes() : null;

        Book book = bookUseCase.updateBook(id, new BookUseCase.UpdateBookCommand(
                title, author, description, price, quantity, imageBytes, categoryIds));

        return ResponseEntity.ok(ApiResponse.success("Cập nhật sách thành công", book));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        bookUseCase.deleteBook(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa sách thành công", null));
    }
}
