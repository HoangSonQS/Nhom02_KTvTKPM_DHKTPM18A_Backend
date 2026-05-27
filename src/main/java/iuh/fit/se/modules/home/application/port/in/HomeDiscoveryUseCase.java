package iuh.fit.se.modules.home.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface HomeDiscoveryUseCase {

    HomeDiscoveryResponse getDiscovery(int limit);

    List<HomeBookResponse> getTrendingBooks(int limit);

    List<HomeBookResponse> getHotBooks(int limit);

    List<HomeBookResponse> getShockSaleBooks(int limit);

    List<HomeBookResponse> getRankingBooks(String type, int limit);

    List<HomeBookResponse> getRecommendations(Long userId, int limit);

    record HomeDiscoveryResponse(
            LocalDate snapshotDate,
            List<HomeSectionResponse> sections
    ) {}

    record HomeSectionResponse(
            String key,
            String title,
            List<HomeBookResponse> books
    ) {}

    record HomeBookResponse(
            Long id,
            String title,
            String author,
            String description,
            BigDecimal price,
            BigDecimal originalPrice,
            Integer discountPercent,
            int quantity,
            String imageUrl,
            boolean active,
            BigDecimal averageRating,
            int ratingCount,
            Set<Long> categoryIds,
            Long quantitySold,
            BigDecimal revenue,
            String badge,
            String reason
    ) {}
}
