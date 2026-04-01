package iuh.fit.se.modules.catalog.application.service;

import iuh.fit.se.modules.catalog.application.port.out.BookImagePort;
import iuh.fit.se.modules.catalog.application.port.out.BookPersistencePort;
import iuh.fit.se.modules.catalog.domain.Book;
import iuh.fit.se.shared.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookPersistencePort, bookImagePort);
    }

    @Test
    void givenBookInStock_whenDecreaseStock_thenSuccess() {
        // Arrange
        Book book = Book.builder()
                .title("Clean Architecture")
                .quantity(10)
                .build();
        when(bookPersistencePort.findById(1L)).thenReturn(Optional.of(book));

        // Act
        bookService.updateStock(1L, 3, false);

        // Assert
        assertThat(book.getQuantity()).isEqualTo(7);
        verify(bookPersistencePort).save(book);
    }

    @Test
    void givenOutOfStock_whenDecreaseStock_thenThrowsException() {
        // Arrange
        Book book = Book.builder().quantity(5).build();
        when(bookPersistencePort.findById(1L)).thenReturn(Optional.of(book));

        // Act & Assert
        assertThatThrownBy(() -> bookService.updateStock(1L, 6, false))
                .isInstanceOf(AppException.class);
        
        verify(bookPersistencePort, never()).save(any());
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
