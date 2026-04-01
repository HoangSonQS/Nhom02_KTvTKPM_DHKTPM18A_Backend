package iuh.fit.se.modules.catalog.application.service;

import iuh.fit.se.modules.catalog.application.port.in.BookUseCase;
import iuh.fit.se.modules.catalog.application.port.out.BookImagePort;
import iuh.fit.se.modules.catalog.application.port.out.BookPersistencePort;
import iuh.fit.se.modules.catalog.domain.Book;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import iuh.fit.se.shared.infrastructure.cloudinary.CloudinaryUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

/**
 * BookService — Implementation của BookUseCase.
 */
@Service
@RequiredArgsConstructor
public class BookService implements BookUseCase {

    private final BookPersistencePort bookPersistencePort;
    private final BookImagePort bookImagePort;

    @Override
    @Transactional
    public Book createBook(CreateBookCommand command) {
        String imageUrl = null;
        String imagePublicId = null;

        if (command.imageFile() != null && command.imageFile().length > 0) {
            CloudinaryUploadResult result = bookImagePort.uploadBookImage(command.imageFile());
            imageUrl = result.url();
            imagePublicId = result.publicId();
        }

        Book book = Book.builder()
                .title(command.title())
                .author(command.author())
                .description(command.description())
                .price(command.price())
                .quantity(command.quantity())
                .imageUrl(imageUrl)
                .imagePublicId(imagePublicId)
                .isActive(true)
                .categoryIds(new HashSet<>(command.categoryIds()))
                .build();

        return bookPersistencePort.save(book);
    }

    @Override
    @Transactional
    public Book updateBook(Long id, UpdateBookCommand command) {
        Book book = getBook(id);

        book.updateBasicInfo(
                command.title(),
                command.author(),
                command.description(),
                command.price(),
                command.quantity(),
                new HashSet<>(command.categoryIds())
        );

        if (command.imageFile() != null && command.imageFile().length > 0) {
            String oldPublicId = book.getImagePublicId();
            CloudinaryUploadResult result = bookImagePort.uploadBookImage(command.imageFile());
            book.updateImage(result.url(), result.publicId());

            if (oldPublicId != null) {
                bookImagePort.deleteBookImage(oldPublicId);
            }
        }

        return bookPersistencePort.save(book);
    }

    @Override
    @Transactional
    public void deleteBook(Long id) {
        Book book = getBook(id);
        String publicId = book.getImagePublicId();

        // 1. Xóa trong DB trước (Source of Truth)
        bookPersistencePort.delete(id);

        // 2. Xóa trên Cloudinary sau khi DB xóa thành công (orphan cleanup)
        if (publicId != null) {
            bookImagePort.deleteBookImage(publicId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Book getBook(Long id) {
        return bookPersistencePort.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy sách"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Book> searchBooks(String title, Long categoryId) {
        return bookPersistencePort.search(title, categoryId);
    }

    @Override
    @Transactional
    public void updateStock(Long id, int amount, boolean isIncrease) {
        Book book = getBook(id);
        if (isIncrease) {
            book.increaseStock(amount);
        } else {
            book.decreaseStock(amount);
        }
        bookPersistencePort.save(book);
    }
}
