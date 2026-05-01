package iuh.fit.se.modules.promotion.adapter.inbound.web;

import iuh.fit.se.modules.promotion.application.port.in.PromotionApplicationResult;
import iuh.fit.se.modules.promotion.application.port.in.PromotionInternalUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionInternalUseCase internalUseCase;

    // ─── Customer Endpoints ───────────────────────────────────────────────────

    /**
     * POST /api/v1/promotions/validate
     * Kiểm tra tính hợp lệ của coupon (Customer). Soft-check — không trừ lượt dùng.
     */
    @PostMapping("/api/v1/promotions/validate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ValidateCouponResponse>> validateCoupon(
            @Valid @RequestBody ValidateCouponRequest req) {
        PromotionApplicationResult result = internalUseCase.validateCoupon(req.code(), req.orderTotal());
        ValidateCouponResponse resp = new ValidateCouponResponse(
                result.isSuccess(),
                result.getDiscountAmount(),
                result.getFinalTotal(),
                result.isSuccess() ? null : result.getErrorMessage()
        );
        return ResponseEntity.ok(ApiResponse.success(
                result.isSuccess() ? "Mã khuyến mãi hợp lệ" : "Mã khuyến mãi không hợp lệ",
                resp
        ));
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────


    record ValidateCouponRequest(
            @NotBlank(message = "Mã coupon không được để trống") String code,
            @NotNull(message = "Tổng đơn hàng không được để trống") @Positive BigDecimal orderTotal
    ) {}

    record ValidateCouponResponse(
            boolean valid,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            String message
    ) {}
}
