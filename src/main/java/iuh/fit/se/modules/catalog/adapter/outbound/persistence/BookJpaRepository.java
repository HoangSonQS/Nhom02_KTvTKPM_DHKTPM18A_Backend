package iuh.fit.se.modules.catalog.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookJpaRepository extends JpaRepository<BookJpaEntity, Long> {

    @Query("SELECT b FROM BookJpaEntity b LEFT JOIN b.categoryIds c " +
           "WHERE (:title IS NULL OR b.title LIKE %:title%) " +
           "AND (:categoryId IS NULL OR c = :categoryId)")
    List<BookJpaEntity> search(@Param("title") String title, @Param("categoryId") Long categoryId);
}
