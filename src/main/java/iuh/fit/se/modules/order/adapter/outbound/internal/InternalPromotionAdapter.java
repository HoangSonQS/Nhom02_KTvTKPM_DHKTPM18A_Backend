package iuh.fit.se.modules.order.adapter.outbound.internal;

import iuh.fit.se.modules.order.application.port.out.PromotionPort;
import iuh.fit.se.modules.promotion.application.port.in.FlashSaleUseCase;
import iuh.fit.se.modules.promotion.application.port.in.PromotionApplicationResult;
import iuh.fit.se.modules.promotion.application.port.in.PromotionInternalUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InternalPromotionAdapter implements PromotionPort {

    private final PromotionInternalUseCase promotionUseCase;
    private final FlashSaleUseCase flashSaleUseCase;

    @Override
    public PromotionPort.PromotionResult reserveCoupon(String code, BigDecimal totalAmount, String referenceId) {
        PromotionApplicationResult result = promotionUseCase.reserveCoupon(code, totalAmount, referenceId);
        
        return PromotionPort.PromotionResult.builder()
                .success(result.isSuccess())
                .discountAmount(result.getDiscountAmount())
                .message(result.getErrorMessage())
                .build();
    }

    @Override
    public void releaseCoupon(String referenceId) {
        promotionUseCase.releaseCoupon(referenceId);
    }

    @Override
    public void confirmCouponUsage(String referenceId) {
        promotionUseCase.confirmCouponUsage(referenceId);
    }

    @Override
    public void reserveFlashSaleItems(List<PromotionPort.FlashSaleItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        items.forEach(item -> flashSaleUseCase.reserveActiveSaleQuantity(item.getBookId(), item.getQuantity()));
    }
}
