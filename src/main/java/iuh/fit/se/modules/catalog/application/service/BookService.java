package iuh.fit.se.modules.catalog.application.service;

import iuh.fit.se.modules.catalog.adapter.outbound.persistence.BookMapper;
import iuh.fit.se.modules.catalog.application.port.in.BookDTO;
import iuh.fit.se.modules.catalog.application.port.in.BookUseCase;
import iuh.fit.se.modules.catalog.application.port.out.BookImagePort;
import iuh.fit.se.modules.catalog.application.port.out.BookPersistencePort;
import iuh.fit.se.modules.catalog.domain.Book;
import iuh.fit.se.shared.event.catalog.BookCreatedEvent;
import iuh.fit.se.shared.event.catalog.BookUpdatedEvent;
import iuh.fit.se.shared.event.catalog.BookDeletedEvent;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import iuh.fit.se.shared.infrastructure.cloudinary.CloudinaryUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BookService — Implementation của BookUseCase.
 */
@Service
@RequiredArgsConstructor
public class BookService implements BookUseCase {

    private final BookPersistencePort bookPersistencePort;
    private final BookImagePort bookImagePort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    @CacheEvict(value = "books", allEntries = true)
    public BookDTO createBook(CreateBookCommand command) {
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
                .deprecatedQuantity(command.quantity())
                .imageUrl(imageUrl)
                .imagePublicId(imagePublicId)
                .isActive(true)
                .categoryIds(new HashSet<>(command.categoryIds()))
                .build();

        Book savedBook = bookPersistencePort.save(book);

        // Publish event for AI module to sync embedding
        eventPublisher.publishEvent(BookCreatedEvent.builder().bookId(savedBook.getId()).build());

        return BookMapper.toDto(savedBook);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "bookDetails", key = "T(iuh.fit.se.shared.cache.CacheKeyUtility).createSaltedKey('bookDetails', #id)"),
            @CacheEvict(value = "books", allEntries = true)
    })
    public BookDTO updateBook(Long id, UpdateBookCommand command) {
        Book book = bookPersistencePort.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy sách"));

        book.updateBasicInfo(
                command.title(),
                command.author(),
                command.description(),
                command.price(),
                command.quantity(),
                new HashSet<>(command.categoryIds()));

        if (command.imageFile() != null && command.imageFile().length > 0) {
            String oldPublicId = book.getImagePublicId();
            CloudinaryUploadResult result = bookImagePort.uploadBookImage(command.imageFile());
            book.updateImage(result.url(), result.publicId());

            if (oldPublicId != null) {
                bookImagePort.deleteBookImage(oldPublicId);
            }
        }

        Book updatedBook = bookPersistencePort.save(book);

        // Phát sự kiện cập nhật
        eventPublisher.publishEvent(BookUpdatedEvent.builder()
                .bookId(updatedBook.getId())
                .title(updatedBook.getTitle())
                .author(updatedBook.getAuthor())
                .description(updatedBook.getDescription())
                .build());

        return BookMapper.toDto(updatedBook);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "bookDetails", key = "T(iuh.fit.se.shared.cache.CacheKeyUtility).createSaltedKey('bookDetails', #id)"),
            @CacheEvict(value = "books", allEntries = true)
    })
    public void deleteBook(Long id) {
        Book book = bookPersistencePort.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy sách"));
        String publicId = book.getImagePublicId();

        bookPersistencePort.delete(id);

        eventPublisher.publishEvent(BookDeletedEvent.builder()
                .bookId(id)
                .build());

        if (publicId != null) {
            bookImagePort.deleteBookImage(publicId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "bookDetails", key = "T(iuh.fit.se.shared.cache.CacheKeyUtility).createSaltedKey('bookDetails', #id)")
    public BookDTO getBook(Long id) {
        Book book = bookPersistencePort.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy sách"));
        return BookMapper.toDto(book);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "books", key = "T(iuh.fit.se.shared.cache.CacheKeyUtility).createSaltedKey('books', 'search:' + #title + ':' + #categoryId)")
    public List<BookDTO> searchBooks(String title, Long categoryId) {
        return bookPersistencePort.search(title, categoryId).stream()
                .map(BookMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "bookDetails", key = "T(iuh.fit.se.shared.cache.CacheKeyUtility).createSaltedKey('bookDetails', #id)"),
            @CacheEvict(value = "books", allEntries = true)
    })
    public void updateStock(Long id, int amount, boolean isIncrease) {
        Book book = bookPersistencePort.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy sách"));

        if (isIncrease) {
            book.increaseStock(amount);
        } else {
            book.decreaseStock(amount);
        }
        bookPersistencePort.save(book);
    }
}
