package iuh.fit.se.modules.home.application.service;

import iuh.fit.se.modules.catalog.application.port.in.BookDTO;
import iuh.fit.se.modules.catalog.application.port.in.BookUseCase;
import iuh.fit.se.modules.cart.application.port.in.CartInternalUseCase;
import iuh.fit.se.modules.home.application.port.in.HomeDiscoveryUseCase;
import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeDiscoveryService implements HomeDiscoveryUseCase {

    private static final int DEFAULT_LIMIT = 8;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final BookUseCase bookUseCase;
    private final OrderInternalUseCase orderUseCase;
    private final CartInternalUseCase cartUseCase;

    @Override
    @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
    public HomeDiscoveryResponse getDiscovery(int limit) {
        int effectiveLimit = normalizeLimit(limit);
        return new HomeDiscoveryResponse(
                LocalDate.now(),
                List.of(
                        new HomeSectionResponse("trending-daily", "Xu huong theo ngay", getTrendingBooks(effectiveLimit)),
                        new HomeSectionResponse("hot-books", "Sach hot", getHotBooks(effectiveLimit)),
                        new HomeSectionResponse("shock-sale", "Giam soc", getShockSaleBooks(effectiveLimit)),
                        new HomeSectionResponse("sales-ranking", "Bang xep hang ban ra", getSalesRankingBooks(effectiveLimit)),
                        new HomeSectionResponse("best-seller-ranking", "Bestseller", getBestSellerBooks(effectiveLimit))
                )
        );
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
    public List<HomeBookResponse> getTrendingBooks(int limit) {
        int effectiveLimit = normalizeLimit(limit);
        LocalDate yesterday = LocalDate.now().minusDays(1);

        return salesRowsToBooks(
                orderUseCase.getBookSales(yesterday, yesterday),
                effectiveLimit,
                "TRENDING",
                sale -> "Da ban " + sale.quantitySold() + " cuon hom qua"
        );
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
    public List<HomeBookResponse> getHotBooks(int limit) {
        int effectiveLimit = normalizeLimit(limit);
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        return salesRowsToBooks(
                orderUseCase.getBookSales(firstDayOfMonth, today),
                effectiveLimit,
                "HOT",
                sale -> "Da ban " + sale.quantitySold() + " cuon trong thang nay"
        );
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
    public List<HomeBookResponse> getShockSaleBooks(int limit) {
        int effectiveLimit = normalizeLimit(limit);
        Map<Long, OrderInternalUseCase.TopSellingBookResponse> sales = getSalesMap(effectiveLimit * 2);

        return allActiveBooks().stream()
                .filter(book -> discountPercent(book) != null && discountPercent(book) >= 30)
                .sorted(Comparator
                        .comparing((BookDTO book) -> discountPercent(book)).reversed()
                        .thenComparing(book -> quantitySold(sales.get(book.id())), Comparator.reverseOrder())
                        .thenComparing(BookDTO::ratingCount, Comparator.reverseOrder()))
                .limit(effectiveLimit)
                .map(book -> toResponse(book, sales.get(book.id()), "SALE", "Giam " + discountPercent(book) + "%"))
                .toList();
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
    public List<HomeBookResponse> getRankingBooks(String type, int limit) {
        String normalizedType = type == null ? "BEST_SELLER" : type.trim().toUpperCase();
        return switch (normalizedType) {
            case "TRENDING" -> getTrendingBooks(limit);
            case "DISCOUNT", "SHOCK_SALE" -> getShockSaleBooks(limit);
            case "SALES", "SALES_RANKING", "SOLD" -> getSalesRankingBooks(limit);
            default -> getBestSellerBooks(limit);
        };
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
    public List<HomeBookResponse> getRecommendations(Long userId, int limit) {
        int effectiveLimit = normalizeLimit(limit);
        List<BookDTO> activeBooks = allActiveBooks();
        Map<Long, BookDTO> booksById = activeBooks.stream()
                .collect(Collectors.toMap(BookDTO::id, Function.identity(), (left, right) -> left));
        Map<Long, OrderInternalUseCase.TopSellingBookResponse> sales = getSalesMap(effectiveLimit * 4);

        Set<Long> seedBookIds = getSeedBookIds(userId);
        Set<Long> purchasedBookIds = getPurchasedBookIds(userId);
        Set<Long> seedCategoryIds = seedBookIds.stream()
                .map(booksById::get)
                .filter(Objects::nonNull)
                .map(BookDTO::categoryIds)
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!seedCategoryIds.isEmpty()) {
            List<HomeBookResponse> personalized = activeBooks.stream()
                    .filter(book -> !seedBookIds.contains(book.id()))
                    .filter(book -> !purchasedBookIds.contains(book.id()))
                    .filter(book -> hasAnyCategory(book, seedCategoryIds))
                    .sorted(Comparator.comparing(
                            (BookDTO book) -> recommendationScore(book, sales.get(book.id()), seedCategoryIds)
                    ).reversed())
                    .limit(effectiveLimit)
                    .map(book -> toResponse(
                            book,
                            sales.get(book.id()),
                            "FOR_YOU",
                            "Gợi ý theo danh mục bạn quan tâm"
                    ))
                    .toList();

            if (personalized.size() >= effectiveLimit) {
                return personalized;
            }

            return appendFallbackRecommendations(personalized, activeBooks, sales, seedBookIds, purchasedBookIds, effectiveLimit);
        }

        return appendFallbackRecommendations(List.of(), activeBooks, sales, Set.of(), purchasedBookIds, effectiveLimit);
    }

    private List<HomeBookResponse> getBestSellerBooks(int limit) {
        int effectiveLimit = normalizeLimit(limit);
        List<OrderInternalUseCase.BookSalesResponse> sales = orderUseCase.getBookSales(null, null);
        List<HomeBookResponse> soldBooks = salesRowsToBooks(
                sales,
                effectiveLimit,
                "BESTSELLER",
                sale -> "Da ban " + sale.quantitySold() + " cuon"
        );

        if (soldBooks.size() >= effectiveLimit) {
            return soldBooks;
        }

        Set<Long> selectedIds = soldBooks.stream()
                .map(HomeBookResponse::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, OrderInternalUseCase.TopSellingBookResponse> salesMap = getSalesMap(effectiveLimit * 2);

        List<HomeBookResponse> fallbackBooks = allActiveBooks().stream()
                .filter(book -> !selectedIds.contains(book.id()))
                .sorted(Comparator.comparing((BookDTO book) -> fallbackPopularityScore(book)).reversed())
                .limit(effectiveLimit - soldBooks.size())
                .map(book -> toResponse(book, salesMap.get(book.id()), "BESTSELLER", recommendationReason(book, salesMap.get(book.id()))))
                .toList();

        return java.util.stream.Stream.concat(soldBooks.stream(), fallbackBooks.stream())
                .limit(effectiveLimit)
                .toList();
    }

    private List<HomeBookResponse> getSalesRankingBooks(int limit) {
        int effectiveLimit = normalizeLimit(limit);
        return salesRowsToBooks(
                orderUseCase.getBookSales(null, null),
                effectiveLimit,
                "SOLD",
                sale -> "Da ban " + sale.quantitySold() + " cuon"
        );
    }

    private Set<Long> getSeedBookIds(Long userId) {
        Set<Long> seedBookIds = new LinkedHashSet<>();
        if (userId == null) {
            return seedBookIds;
        }

        try {
            cartUseCase.getCartByUserId(userId).getItems().forEach(item -> seedBookIds.add(item.getBookId()));
        } catch (RuntimeException ignored) {
            // Recommendations should never break the homepage.
        }

        try {
            orderUseCase.getMyOrders(userId).stream()
                    .filter(order -> order.getItems() != null)
                    .filter(order -> !"CANCELLED".equalsIgnoreCase(order.getFulfillmentStatus()))
                    .flatMap(order -> order.getItems().stream())
                    .forEach(item -> seedBookIds.add(item.getBookId()));
        } catch (RuntimeException ignored) {
            // Fallback recommendations are enough if order history is unavailable.
        }

        return seedBookIds;
    }

    private Set<Long> getPurchasedBookIds(Long userId) {
        Set<Long> purchasedBookIds = new HashSet<>();
        if (userId == null) {
            return purchasedBookIds;
        }

        try {
            orderUseCase.getMyOrders(userId).stream()
                    .filter(order -> "DELIVERED".equalsIgnoreCase(order.getFulfillmentStatus()))
                    .filter(order -> order.getItems() != null)
                    .flatMap(order -> order.getItems().stream())
                    .forEach(item -> purchasedBookIds.add(item.getBookId()));
        } catch (RuntimeException ignored) {
            // Keep homepage resilient.
        }

        return purchasedBookIds;
    }

    private List<HomeBookResponse> appendFallbackRecommendations(
            List<HomeBookResponse> priority,
            List<BookDTO> activeBooks,
            Map<Long, OrderInternalUseCase.TopSellingBookResponse> sales,
            Set<Long> seedBookIds,
            Set<Long> purchasedBookIds,
            int limit
    ) {
        Set<Long> selectedIds = priority.stream()
                .map(HomeBookResponse::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<HomeBookResponse> fallback = activeBooks.stream()
                .filter(book -> !selectedIds.contains(book.id()))
                .filter(book -> !seedBookIds.contains(book.id()))
                .filter(book -> !purchasedBookIds.contains(book.id()))
                .sorted(Comparator.comparing((BookDTO book) -> fallbackRecommendationScore(book, sales.get(book.id()))).reversed())
                .limit(Math.max(0, limit - priority.size()))
                .map(book -> toResponse(
                        book,
                        sales.get(book.id()),
                        "RECOMMENDED",
                        recommendationReason(book, sales.get(book.id()))
                ))
                .toList();

        return java.util.stream.Stream.concat(priority.stream(), fallback.stream())
                .limit(limit)
                .toList();
    }

    private boolean hasAnyCategory(BookDTO book, Set<Long> categoryIds) {
        if (book.categoryIds() == null || book.categoryIds().isEmpty()) {
            return false;
        }
        return book.categoryIds().stream().anyMatch(categoryIds::contains);
    }

    private BigDecimal recommendationScore(
            BookDTO book,
            OrderInternalUseCase.TopSellingBookResponse sale,
            Set<Long> seedCategoryIds
    ) {
        long categoryMatches = book.categoryIds() == null
                ? 0
                : book.categoryIds().stream().filter(seedCategoryIds::contains).count();
        return fallbackRecommendationScore(book, sale)
                .add(BigDecimal.valueOf(categoryMatches).multiply(BigDecimal.valueOf(60)));
    }

    private BigDecimal fallbackRecommendationScore(BookDTO book, OrderInternalUseCase.TopSellingBookResponse sale) {
        BigDecimal score = fallbackPopularityScore(book);
        score = score.add(BigDecimal.valueOf(quantitySold(sale)).multiply(BigDecimal.valueOf(10)));
        if (book.createdAt() != null && book.createdAt().isAfter(LocalDateTime.now().minusDays(14))) {
            score = score.add(BigDecimal.valueOf(35));
        }
        return score;
    }

    private String recommendationReason(BookDTO book, OrderInternalUseCase.TopSellingBookResponse sale) {
        if (quantitySold(sale) > 0) {
            return "Đang được nhiều độc giả chọn mua";
        }
        if (book.createdAt() != null && book.createdAt().isAfter(LocalDateTime.now().minusDays(14))) {
            return "Sách mới đáng chú ý";
        }
        if (discountPercent(book) != null) {
            return "Giá tốt hôm nay";
        }
        return "Phù hợp để khám phá thêm";
    }

    private List<BookDTO> allActiveBooks() {
        return bookUseCase.searchBooks(null, null).stream()
                .filter(BookDTO::isActive)
                .toList();
    }

    private Optional<BookDTO> findBook(Long bookId) {
        try {
            return Optional.of(bookUseCase.getBook(bookId));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private Map<Long, OrderInternalUseCase.TopSellingBookResponse> getSalesMap(int limit) {
        return orderUseCase.getTopSellingBooks(Math.max(DEFAULT_LIMIT, limit)).stream()
                .collect(Collectors.toMap(
                        OrderInternalUseCase.TopSellingBookResponse::bookId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private HomeBookResponse toResponse(
            BookDTO book,
            OrderInternalUseCase.TopSellingBookResponse sale,
            String badge,
            String reason
    ) {
        return new HomeBookResponse(
                book.id(),
                book.title(),
                book.author(),
                book.description(),
                book.price(),
                book.originalPrice(),
                discountPercent(book),
                book.quantity(),
                book.imageUrl(),
                book.isActive(),
                book.averageRating(),
                book.ratingCount(),
                book.categoryIds(),
                sale != null ? sale.quantitySold() : null,
                sale != null ? sale.revenue() : null,
                badge,
                reason
        );
    }

    private HomeBookResponse toResponse(
            BookDTO book,
            OrderInternalUseCase.BookSalesResponse sale,
            String badge,
            String reason
    ) {
        return new HomeBookResponse(
                book.id(),
                book.title(),
                book.author(),
                book.description(),
                book.price(),
                book.originalPrice(),
                discountPercent(book),
                book.quantity(),
                book.imageUrl(),
                book.isActive(),
                book.averageRating(),
                book.ratingCount(),
                book.categoryIds(),
                sale != null ? sale.quantitySold() : null,
                sale != null ? sale.revenue() : null,
                badge,
                reason
        );
    }

    private List<HomeBookResponse> salesRowsToBooks(
            List<OrderInternalUseCase.BookSalesResponse> sales,
            int limit,
            String badge,
            Function<OrderInternalUseCase.BookSalesResponse, String> reasonFactory
    ) {
        Map<Long, BookDTO> booksById = allActiveBooks().stream()
                .collect(Collectors.toMap(BookDTO::id, Function.identity(), (left, right) -> left));

        return sales.stream()
                .limit(limit)
                .map(row -> Optional.ofNullable(booksById.get(row.bookId()))
                        .or(() -> findBook(row.bookId()).filter(BookDTO::isActive))
                        .map(book -> toResponse(book, row, badge, reasonFactory.apply(row)))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private BigDecimal trendingScore(BookDTO book, OrderInternalUseCase.TopSellingBookResponse sale) {
        BigDecimal score = BigDecimal.ZERO;
        score = score.add(BigDecimal.valueOf(quantitySold(sale)).multiply(BigDecimal.valueOf(12)));
        score = score.add(BigDecimal.valueOf(book.ratingCount()).multiply(BigDecimal.valueOf(2)));
        score = score.add(safeNumber(book.averageRating()).multiply(BigDecimal.valueOf(5)));
        score = score.add(BigDecimal.valueOf(Math.max(book.quantity(), 0)).min(BigDecimal.valueOf(20)));
        Integer discount = discountPercent(book);
        if (discount != null) {
            score = score.add(BigDecimal.valueOf(discount));
        }
        return score;
    }

    private BigDecimal fallbackPopularityScore(BookDTO book) {
        BigDecimal score = safeNumber(book.averageRating()).multiply(BigDecimal.valueOf(10));
        score = score.add(BigDecimal.valueOf(book.ratingCount()).multiply(BigDecimal.valueOf(3)));
        Integer discount = discountPercent(book);
        if (discount != null) {
            score = score.add(BigDecimal.valueOf(discount));
        }
        return score;
    }

    private long quantitySold(OrderInternalUseCase.TopSellingBookResponse sale) {
        return sale != null ? sale.quantitySold() : 0L;
    }

    private Integer discountPercent(BookDTO book) {
        BigDecimal price = safeNumber(book.price());
        BigDecimal originalPrice = safeNumber(book.originalPrice());
        if (originalPrice.compareTo(BigDecimal.ZERO) <= 0 || originalPrice.compareTo(price) <= 0) {
            return null;
        }
        return originalPrice.subtract(price)
                .multiply(ONE_HUNDRED)
                .divide(originalPrice, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private BigDecimal safeNumber(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, 100);
    }
}
