package iuh.fit.se.modules.catalog.application.service;

import iuh.fit.se.modules.catalog.application.port.in.BookDTO;
import iuh.fit.se.modules.catalog.application.port.in.BookUseCase;
import iuh.fit.se.modules.catalog.application.port.out.BookImagePort;
import iuh.fit.se.modules.catalog.application.port.out.BookPersistencePort;
import iuh.fit.se.modules.inventory.application.port.in.InventoryInternalUseCase;
import iuh.fit.se.modules.catalog.domain.Book;
import iuh.fit.se.shared.event.catalog.BookCreatedEvent;
import iuh.fit.se.shared.event.catalog.BookUpdatedEvent;
import iuh.fit.se.shared.event.catalog.BookDeletedEvent;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import iuh.fit.se.shared.infrastructure.cloudinary.CloudinaryUploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BookService — Implementation của BookUseCase.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookService implements BookUseCase {

    private final BookPersistencePort bookPersistencePort;
    private final BookImagePort bookImagePort;
    private final ApplicationEventPublisher eventPublisher;
    private final InventoryInternalUseCase inventoryInternalUseCase;

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "STAFF_CREATE_BOOK")
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

        // Áp dụng metadata bổ sung
        Set<String> keywords = command.keywords() != null ? new HashSet<>(command.keywords()) : new HashSet<>();
        book.updateMetadata(
                command.publisher(), command.isbn(), command.publicationYear(),
                command.language(), keywords,
                command.pageCount(), command.coverType(),
                command.weight(), command.length(), command.width(), command.height(),
                command.originalPrice());

        Book savedBook = bookPersistencePort.save(book);

        // Publish event for AI module and Inventory module
        eventPublisher.publishEvent(BookCreatedEvent.builder()
                .bookId(savedBook.getId())
                .title(savedBook.getTitle())
                .author(savedBook.getAuthor())
                .price(savedBook.getPrice())
                .initialQuantity(command.quantity())
                .build());

        return BookDtoMapper.toDto(savedBook);
    }

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "STAFF_UPDATE_BOOK")
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
                command.categoryIds() != null ? new HashSet<>(command.categoryIds()) : null);

        // Cập nhật metadata bổ sung (null = giữ nguyên giá trị cũ)
        Set<String> keywords = command.keywords() != null ? new HashSet<>(command.keywords()) : null;
        book.updateMetadata(
                command.publisher(), command.isbn(), command.publicationYear(),
                command.language(), keywords,
                command.pageCount(), command.coverType(),
                command.weight(), command.length(), command.width(), command.height(),
                command.originalPrice());

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

        return BookDtoMapper.toDto(updatedBook);
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
        
        // Fetch real stock from Inventory Source of Truth
        int realStock = 0;
        try {
            realStock = inventoryInternalUseCase.getAvailableStock(id).getRemainingQuantity();
        } catch (Exception e) {
            log.error("Failed to fetch stock for book {}: {}", id, e.getMessage());
            realStock = book.getDeprecatedQuantity(); // Fallback to stale local data
        }
        
        return BookDtoMapper.toDto(book, realStock);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "books", key = "T(iuh.fit.se.shared.cache.CacheKeyUtility).createSaltedKey('books', 'search:' + #title + ':' + #categoryId)")
    public List<BookDTO> searchBooks(String title, Long categoryId) {
        List<Book> books = bookPersistencePort.search(title, categoryId);
        List<Long> bookIds = books.stream().map(Book::getId).collect(Collectors.toList());

        // Bulk fetch real stocks from Inventory Source of Truth
        java.util.Map<Long, Integer> stocks = new java.util.HashMap<>();
        try {
            stocks = inventoryInternalUseCase.getAvailableStocks(bookIds);
        } catch (Exception e) {
            log.error("Failed to bulk fetch stocks: {}", e.getMessage());
            // Map will be empty, will fallback to deprecatedQuantity in Mapper
        }

        final java.util.Map<Long, Integer> finalStocks = stocks;
        return books.stream()
                .map(book -> BookDtoMapper.toDto(book, finalStocks.get(book.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateStock(Long id, int amount, boolean isIncrease) {
        // Kiểm tra sách tồn tại
        if (!bookPersistencePort.existsById(id)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy sách");
        }

        // Ủy quyền sang module Inventory (Source of Truth mới)
        String referenceId = "CAT_MANUAL_UPDATE_" + java.util.UUID.randomUUID();
        if (isIncrease) {
            inventoryInternalUseCase.increaseStock(id, amount, referenceId);
        } else {
            inventoryInternalUseCase.decreaseStock(id, amount, referenceId);
        }
        
        // Note: Field deprecatedQuantity trong cat_book không còn được cập nhật thủ công ở đây.
        // Nó sẽ được sync định kỳ hoặc xóa bỏ hoàn toàn ở Phase sau.
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "bookDetails", key = "T(iuh.fit.se.shared.cache.CacheKeyUtility).createSaltedKey('bookDetails', #id)"),
            @CacheEvict(value = "books", allEntries = true)
    })
    public void syncStock(Long id, int quantity) {
        bookPersistencePort.findById(id).ifPresent(book -> {
            book.syncQuantity(quantity);
            bookPersistencePort.save(book);
            log.info("Synced stock for book {} to new quantity {}", id, quantity);
        });
    }
}
