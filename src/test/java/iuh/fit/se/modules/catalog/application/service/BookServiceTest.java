package iuh.fit.se.modules.catalog.application.service;

import iuh.fit.se.modules.catalog.application.port.out.BookImagePort;
import iuh.fit.se.modules.catalog.application.port.out.BookPersistencePort;
import iuh.fit.se.modules.catalog.domain.Book;
import iuh.fit.se.modules.inventory.application.port.in.InventoryInternalUseCase;
import iuh.fit.se.shared.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    private BookService bookService;

    @Mock
    private BookPersistencePort bookPersistencePort;
    @Mock
    private BookImagePort bookImagePort;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private InventoryInternalUseCase inventoryInternalUseCase;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookPersistencePort, bookImagePort, eventPublisher, inventoryInternalUseCase);
    }

    @Test
    void givenBookExists_whenUpdateStock_thenDelegatesToInventory() {
        // Arrange
        when(bookPersistencePort.existsById(1L)).thenReturn(true);

        // Act
        bookService.updateStock(1L, 5, true);

        // Assert
        verify(inventoryInternalUseCase).increaseStock(eq(1L), eq(5), anyString());
        verify(bookPersistencePort, never()).save(any());
    }

    @Test
    void givenBookNotFound_whenUpdateStock_thenThrowsException() {
        // Arrange
        when(bookPersistencePort.existsById(1L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> bookService.updateStock(1L, 5, true))
                .isInstanceOf(AppException.class);
    }

    @Test
    void givenBookWithImage_whenDeleteBook_thenDBDeletedAndCloudinaryCalled() {
        // Arrange
        Book book = Book.builder()
                .id(1L)
                .imagePublicId("book-img-id")
                .build();
        when(bookPersistencePort.findById(1L)).thenReturn(Optional.of(book));

        // Act
        bookService.deleteBook(1L);

        // Assert
        verify(bookPersistencePort).delete(1L);
        verify(bookImagePort).deleteBookImage("book-img-id");
    }
}
