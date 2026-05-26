package iuh.fit.se.modules.catalog.application.service;

import iuh.fit.se.modules.catalog.application.port.in.BookDTO;
import iuh.fit.se.modules.catalog.domain.Book;

import java.util.HashSet;

final class BookDtoMapper {

    private BookDtoMapper() {
    }

    static BookDTO toDto(Book domain) {
        return toDto(domain, null);
    }

    static BookDTO toDto(Book domain, Integer realQuantity) {
        if (domain == null) {
            return null;
        }

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
}
