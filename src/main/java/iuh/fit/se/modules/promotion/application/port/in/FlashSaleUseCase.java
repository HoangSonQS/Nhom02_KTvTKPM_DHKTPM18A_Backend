package iuh.fit.se.modules.promotion.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface FlashSaleUseCase {

    List<FlashSaleResponse> getAll();

    ActiveFlashSaleResponse getActive();

    BigDecimal resolveActiveSalePrice(Long bookId, BigDecimal regularPrice);

    BigDecimal reserveActiveSalePrice(Long bookId, int quantity, BigDecimal regularPrice);

    BigDecimal reserveActiveSalePriceOrRegular(Long bookId, int quantity, BigDecimal regularPrice);

    void reserveActiveSaleQuantity(Long bookId, int quantity);

    FlashSaleResponse create(FlashSaleCommand command);

    FlashSaleResponse update(Long id, FlashSaleCommand command);

    void delete(Long id);

    record FlashSaleCommand(
            Long bookId,
            int saleQuantity,
            int discountPercent,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean active
    ) {}

    record FlashSaleResponse(
            Long id,
            Long bookId,
            String title,
            String author,
            String imageUrl,
            BigDecimal price,
            BigDecimal salePrice,
            int saleQuantity,
            int discountPercent,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean active
    ) {}

    record ActiveFlashSaleResponse(
            LocalDateTime startAt,
            LocalDateTime endAt,
            List<FlashSaleResponse> items
    ) {}
}
