package iuh.fit.se.modules.promotion.application.port.out;

import iuh.fit.se.modules.promotion.domain.FlashSale;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FlashSalePersistencePort {

    List<FlashSale> findAllByOrderByCreatedAtDesc();

    List<FlashSale> findVisibleToday(int saleQuantity, LocalDateTime dayStart, LocalDateTime nextDayStart, LocalDateTime now);

    Optional<FlashSale> findActiveSale(Long bookId, int saleQuantity, LocalDateTime now);

    Optional<FlashSale> findById(Long id);

    FlashSale save(FlashSale sale);

    void deleteById(Long id);
}
