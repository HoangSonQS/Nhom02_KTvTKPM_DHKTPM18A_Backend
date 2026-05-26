package iuh.fit.se.modules.promotion.application.service;

import iuh.fit.se.modules.catalog.application.port.in.BookDTO;
import iuh.fit.se.modules.catalog.application.port.in.BookUseCase;
import iuh.fit.se.modules.promotion.application.port.in.FlashSaleUseCase;
import iuh.fit.se.modules.promotion.application.port.out.FlashSalePersistencePort;
import iuh.fit.se.modules.promotion.domain.FlashSale;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlashSaleService implements FlashSaleUseCase {

    private final FlashSalePersistencePort persistencePort;
    private final BookUseCase bookUseCase;

    @Override
    @Transactional(readOnly = true)
    public List<FlashSaleResponse> getAll() {
        return persistencePort.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ActiveFlashSaleResponse getActive() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime nextDayStart = dayStart.plusDays(1);
        List<FlashSaleResponse> items = persistencePort
                .findVisibleToday(0, dayStart, nextDayStart, now)
                .stream()
                .map(this::toResponse)
                .toList();
        LocalDateTime startAt = items.stream().map(FlashSaleResponse::startAt).min(Comparator.naturalOrder()).orElse(null);
        LocalDateTime endAt = items.stream().map(FlashSaleResponse::endAt).min(Comparator.naturalOrder()).orElse(null);
        return new ActiveFlashSaleResponse(startAt, endAt, items);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal resolveActiveSalePrice(Long bookId, BigDecimal regularPrice) {
        BigDecimal basePrice = regularPrice != null ? regularPrice : BigDecimal.ZERO;
        if (bookId == null) {
            return basePrice;
        }

        LocalDateTime now = LocalDateTime.now();
        return persistencePort
                .findActiveSale(bookId, 0, now)
                .map(sale -> calculateSalePrice(basePrice, sale.getDiscountPercent()))
                .orElse(basePrice);
    }

    @Override
    @Transactional
    public BigDecimal reserveActiveSalePrice(Long bookId, int quantity, BigDecimal regularPrice) {
        BigDecimal basePrice = regularPrice != null ? regularPrice : BigDecimal.ZERO;
        if (bookId == null || quantity <= 0) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Thong tin Flash Sale khong hop le");
        }

        LocalDateTime now = LocalDateTime.now();
        FlashSale sale = persistencePort
                .findActiveSale(bookId, 0, now)
                .orElseThrow(() -> new AppException(ErrorCode.PRM_COUPON_EXPIRED, "Flash Sale da het han hoac het so luong"));

        return calculateSalePrice(basePrice, sale.getDiscountPercent());
    }

    @Override
    @Transactional
    public BigDecimal reserveActiveSalePriceOrRegular(Long bookId, int quantity, BigDecimal regularPrice) {
        BigDecimal basePrice = regularPrice != null ? regularPrice : BigDecimal.ZERO;
        if (bookId == null || quantity <= 0) {
            return basePrice;
        }

        LocalDateTime now = LocalDateTime.now();
        return persistencePort
                .findActiveSale(bookId, 0, now)
                .map(sale -> {
                    return calculateSalePrice(basePrice, sale.getDiscountPercent());
                })
                .orElse(basePrice);
    }

    @Override
    @Transactional
    public void reserveActiveSaleQuantity(Long bookId, int quantity) {
        if (bookId == null || quantity <= 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        persistencePort
                .findActiveSale(bookId, 0, now)
                .ifPresent(sale -> {
                    reserveQuantity(sale, quantity);
                    persistencePort.save(sale);
                });
    }

    @Override
    @Transactional
    public FlashSaleResponse create(FlashSaleCommand command) {
        validate(command);
        FlashSale sale = FlashSale.builder()
                .bookId(command.bookId())
                .saleQuantity(command.saleQuantity())
                .discountPercent(command.discountPercent())
                .startAt(command.startAt())
                .endAt(command.endAt())
                .active(command.active())
                .build();
        return toResponse(persistencePort.save(sale));
    }

    @Override
    @Transactional
    public FlashSaleResponse update(Long id, FlashSaleCommand command) {
        validate(command);
        FlashSale sale = persistencePort.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Khong tim thay flash sale"));
        sale.update(command.bookId(), command.saleQuantity(), command.discountPercent(), command.startAt(), command.endAt(), command.active());
        return toResponse(persistencePort.save(sale));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        persistencePort.deleteById(id);
    }

    private void validate(FlashSaleCommand command) {
        if (command.bookId() == null) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Vui long chon sach");
        }
        if (command.saleQuantity() <= 0) {
            throw new AppException(ErrorCode.INVALID_INPUT, "So luong sale phai lon hon 0");
        }
        if (command.discountPercent() <= 0 || command.discountPercent() > 90) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Phan tram giam phai tu 1 den 90");
        }
        if (command.startAt() == null || command.endAt() == null || !command.endAt().isAfter(command.startAt())) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Thoi gian sale khong hop le");
        }
        bookUseCase.getBook(command.bookId());
    }

    private FlashSaleResponse toResponse(FlashSale sale) {
        BookDTO book = bookUseCase.getBook(sale.getBookId());
        BigDecimal price = book.price() != null ? book.price() : BigDecimal.ZERO;
        BigDecimal salePrice = calculateSalePrice(price, sale.getDiscountPercent());
        return new FlashSaleResponse(
                sale.getId(),
                sale.getBookId(),
                book.title(),
                book.author(),
                book.imageUrl(),
                price,
                salePrice,
                sale.getSaleQuantity(),
                sale.getDiscountPercent(),
                sale.getStartAt(),
                sale.getEndAt(),
                sale.isActive()
        );
    }

    private BigDecimal calculateSalePrice(BigDecimal price, int discountPercent) {
        return price
                .multiply(BigDecimal.valueOf(100L - discountPercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    }

    private void reserveQuantity(FlashSale sale, int quantity) {
        if (sale.getSaleQuantity() < quantity) {
            throw new AppException(ErrorCode.PRM_COUPON_EXPIRED, "Flash Sale khong du so luong");
        }
        try {
            sale.reserve(quantity);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new AppException(ErrorCode.PRM_COUPON_EXPIRED, e.getMessage());
        }
    }
}
