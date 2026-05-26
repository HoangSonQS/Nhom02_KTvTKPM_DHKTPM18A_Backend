package iuh.fit.se.modules.catalog.adapter.outbound.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.modules.catalog.application.port.in.BookDTO;
import iuh.fit.se.modules.catalog.domain.Book;
import iuh.fit.se.modules.catalog.domain.BookContent;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

/**
 * BookMapper — Chuyển đổi giữa Domain Model và JPA Entities (Strict Purity).
 * Cũng hỗ trợ mapping sang DTO cho giao tiếp liên module.
 */
@Slf4j
public class BookMapper {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static BookDTO toDto(Book domain) {
        return toDto(domain, null);
    }

    public static BookDTO toDto(Book domain, Integer realQuantity) {
        if (domain == null)
            return null;

        return BookDTO.builder()
                .id(domain.getId())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .title(domain.getTitle())
                .author(domain.getAuthor())
                .description(domain.getDescription())
                .price(domain.getPrice())
                .quantity(realQuantity != null ? realQuantity : domain.getDeprecatedQuantity())
                .imageUrl(domain.getImageUrl())
                .isActive(domain.isActive())
                .publisher(domain.getPublisher())
                .isbn(domain.getIsbn())
                .publicationYear(domain.getPublicationYear())
                .language(domain.getLanguage())
                .keywords(domain.getKeywords() != null ? new HashSet<>(domain.getKeywords()) : new HashSet<>())
                .pageCount(domain.getPageCount())
                .coverType(domain.getCoverType())
                .weight(domain.getWeight())
                .length(domain.getLength())
                .width(domain.getWidth())
                .height(domain.getHeight())
                .originalPrice(domain.getOriginalPrice())
                .averageRating(domain.getAverageRating())
                .ratingCount(domain.getRatingCount())
                .tableOfContents(domain.getContent() != null ? domain.getContent().getTableOfContents() : null)
                .excerpt(domain.getContent() != null ? domain.getContent().getExcerpt() : null)
                .categoryIds(new HashSet<>(domain.getCategoryIds()))
                .build();
    }

    public static Book toDomain(BookJpaEntity entity, BookContentJpaEntity contentEntity) {
        if (entity == null)
            return null;

        Book.BookBuilder builder = Book.builder()
                .id(entity.getId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .title(entity.getTitle())
                .author(entity.getAuthor())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .deprecatedQuantity(entity.getDeprecatedQuantity())
                .imageUrl(entity.getImageUrl())
                .imagePublicId(entity.getImagePublicId())
                .isActive(entity.isActive())
                .publisher(entity.getPublisher())
                .isbn(entity.getIsbn())
                .publicationYear(entity.getPublicationYear())
                .language(entity.getLanguage())
                .keywords(parseKeywords(entity.getKeywords()))
                .pageCount(entity.getPageCount())
                .coverType(entity.getCoverType())
                .weight(entity.getWeight())
                .length(entity.getLength())
                .width(entity.getWidth())
                .height(entity.getHeight())
                .originalPrice(entity.getOriginalPrice())
                .averageRating(entity.getAverageRating())
                .ratingCount(entity.getRatingCount())
                .categoryIds(new HashSet<>(entity.getCategoryIds()));

        if (contentEntity != null) {
            builder.content(new BookContent(contentEntity.getTableOfContents(), contentEntity.getExcerpt()));
        }

        return builder.build();
    }

    public static BookJpaEntity toJpa(Book domain, BookJpaEntity existingEntity) {
        BookJpaEntity entity = existingEntity != null ? existingEntity : new BookJpaEntity();

        entity.setTitle(domain.getTitle());
        entity.setAuthor(domain.getAuthor());
        entity.setDescription(domain.getDescription());
        entity.setPrice(domain.getPrice());
        entity.setDeprecatedQuantity(domain.getDeprecatedQuantity());
        entity.setImageUrl(domain.getImageUrl());
        entity.setImagePublicId(domain.getImagePublicId());
        entity.setActive(domain.isActive());

        entity.setPublisher(domain.getPublisher());
        entity.setIsbn(domain.getIsbn());
        entity.setPublicationYear(domain.getPublicationYear());
        entity.setLanguage(domain.getLanguage());
        entity.setKeywords(serializeKeywords(domain.getKeywords()));

        entity.setPageCount(domain.getPageCount());
        entity.setCoverType(domain.getCoverType());
        entity.setWeight(domain.getWeight());
        entity.setLength(domain.getLength());
        entity.setWidth(domain.getWidth());
        entity.setHeight(domain.getHeight());

        entity.setOriginalPrice(domain.getOriginalPrice());
        entity.setAverageRating(domain.getAverageRating());
        entity.setRatingCount(domain.getRatingCount());

        entity.setCategoryIds(new HashSet<>(domain.getCategoryIds()));

        return entity;
    }

    public static BookContentJpaEntity toContentJpa(Book domain) {
        if (domain.getContent() == null)
            return null;

        return BookContentJpaEntity.builder()
                .bookId(domain.getId())
                .tableOfContents(domain.getContent().getTableOfContents())
                .excerpt(domain.getContent().getExcerpt())
                .build();
    }

    private static Set<String> parseKeywords(String keywordsJson) {
        if (keywordsJson == null || keywordsJson.isEmpty())
            return new HashSet<>();
        try {
            return objectMapper.readValue(keywordsJson, new TypeReference<Set<String>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to parse keywords JSON: {}", keywordsJson, e);
            return new HashSet<>();
        }
    }

    private static String serializeKeywords(Set<String> keywords) {
        if (keywords == null || keywords.isEmpty())
            return "[]";
        try {
            return objectMapper.writeValueAsString(keywords);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize keywords: {}", keywords, e);
            return "[]";
        }
    }
}
