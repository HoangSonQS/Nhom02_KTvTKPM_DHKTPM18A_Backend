package iuh.fit.se.modules.promotion.application.port.in;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PromotionApplicationResult {
    private boolean isSuccess;
    private BigDecimal originalTotal;
    private BigDecimal discountAmount;
    private BigDecimal finalTotal;
    private List<String> appliedRules;
    private String traceId;
    private String errorMessage;

    public static PromotionApplicationResult success(BigDecimal original, BigDecimal discount, List<String> rules, String traceId) {
        return PromotionApplicationResult.builder()
                .isSuccess(true)
                .originalTotal(original)
                .discountAmount(discount)
                .finalTotal(original.subtract(discount))
                .appliedRules(rules)
                .traceId(traceId)
                .build();
    }

    public static PromotionApplicationResult failure(String errorMessage, BigDecimal originalTotal, String traceId) {
        return PromotionApplicationResult.builder()
                .isSuccess(false)
                .originalTotal(originalTotal)
                .discountAmount(BigDecimal.ZERO)
                .finalTotal(originalTotal)
                .errorMessage(errorMessage)
                .traceId(traceId)
                .build();
    }
}
