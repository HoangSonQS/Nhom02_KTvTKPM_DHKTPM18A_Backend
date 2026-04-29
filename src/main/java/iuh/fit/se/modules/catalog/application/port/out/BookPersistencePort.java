package iuh.fit.se.modules.catalog.application.port.out;

import iuh.fit.se.modules.catalog.domain.Book;
import java.util.List;
import java.util.Optional;

/**
 * BookPersistencePort — Outbound Port cho Book.
 */
public interface BookPersistencePort {

    Optional<Book> findById(Long id);
    
    boolean existsById(Long id);

    Book save(Book book);

    void delete(Long id);

    List<Book> search(String title, Long categoryId);
}
