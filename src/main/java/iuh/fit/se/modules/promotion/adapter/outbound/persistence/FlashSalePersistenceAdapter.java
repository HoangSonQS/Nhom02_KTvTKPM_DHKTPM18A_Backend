package iuh.fit.se.modules.promotion.adapter.outbound.persistence;

import iuh.fit.se.modules.promotion.application.port.out.FlashSalePersistencePort;
import iuh.fit.se.modules.promotion.domain.FlashSale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FlashSalePersistenceAdapter implements FlashSalePersistencePort {

    private final FlashSaleJpaRepository repository;

    @Override
    public List<FlashSale> findAllByOrderByCreatedAtDesc() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public List<FlashSale> findVisibleToday(int saleQuantity, LocalDateTime dayStart, LocalDateTime nextDayStart, LocalDateTime now) {
        return repository.findVisibleToday(saleQuantity, dayStart, nextDayStart, now);
    }

    @Override
    public Optional<FlashSale> findActiveSale(Long bookId, int saleQuantity, LocalDateTime now) {
        return repository.findFirstByBookIdAndActiveTrueAndSaleQuantityGreaterThanAndStartAtLessThanEqualAndEndAtGreaterThanOrderByDiscountPercentDesc(
                bookId,
                saleQuantity,
                now,
                now
        );
    }

    @Override
    public Optional<FlashSale> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public FlashSale save(FlashSale sale) {
        return repository.save(sale);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
