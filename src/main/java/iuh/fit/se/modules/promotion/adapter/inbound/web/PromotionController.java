package iuh.fit.se.modules.promotion.adapter.inbound.web;

import iuh.fit.se.modules.promotion.application.port.in.PromotionAdminUseCase;
import iuh.fit.se.modules.promotion.application.port.in.PromotionApplicationResult;
import iuh.fit.se.modules.promotion.application.port.in.PromotionInternalUseCase;
import iuh.fit.se.modules.promotion.domain.Coupon;
import iuh.fit.se.modules.promotion.domain.DiscountType;
import iuh.fit.se.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionAdminUseCase adminUseCase;
    private final PromotionInternalUseCase internalUseCase;

    // ─── Admin Endpoints ─────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/coupons
     * Lấy toàn bộ danh sách coupon (Admin only).
     */
    @GetMapping("/api/v1/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getAllCoupons() {
        List<CouponResponse> coupons = adminUseCase.getAllCoupons()
                .stream()
                .map(CouponResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Danh sách mã khuyến mãi", coupons));
    }

    /**
     * GET /api/v1/admin/coupons/{id}
     * Lấy chi tiết một coupon theo ID (Admin only).
     */
    @GetMapping("/api/v1/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CouponResponse>> getCouponById(@PathVariable Long id) {
        Coupon coupon = adminUseCase.getCouponById(id);
        return ResponseEntity.ok(ApiResponse.success("Chi tiết mã khuyến mãi", CouponResponse.from(coupon)));
    }

    /**
     * POST /api/v1/admin/coupons
     * Tạo mới coupon (Admin only).
     */
    @PostMapping("/api/v1/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(@Valid @RequestBody CreateCouponRequest req) {
        PromotionAdminUseCase.CreateCouponCommand cmd = new PromotionAdminUseCase.CreateCouponCommand(
                req.code(), req.description(), req.discountType(), req.discountValue(),
                req.minOrderValue(), req.maxDiscountValue(), req.usageLimit(),
                req.startDate(), req.endDate(), req.isActive()
        );
        Coupon created = adminUseCase.createCoupon(cmd);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo mã khuyến mãi thành công", CouponResponse.from(created)));
    }

    /**
     * PUT /api/v1/admin/coupons/{id}
     * Cập nhật coupon (Admin only). Không thể đổi code hay discountType.
     */
    @PutMapping("/api/v1/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CouponResponse>> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCouponRequest req) {
        PromotionAdminUseCase.UpdateCouponCommand cmd = new PromotionAdminUseCase.UpdateCouponCommand(
                req.description(), req.discountValue(), req.minOrderValue(),
                req.maxDiscountValue(), req.usageLimit(), req.startDate(), req.endDate(), req.isActive()
        );
        Coupon updated = adminUseCase.updateCoupon(id, cmd);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật mã khuyến mãi thành công", CouponResponse.from(updated)));
    }

    /**
     * DELETE /api/v1/admin/coupons/{id}
     * Xóa coupon (Admin only).
     */
    @DeleteMapping("/api/v1/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable Long id) {
        adminUseCase.deleteCoupon(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa mã khuyến mãi thành công", null));
    }

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

    record CouponResponse(
            Long id,
            String code,
            String description,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal minOrderValue,
            BigDecimal maxDiscountValue,
            Integer usageLimit,
            int usedCount,
            LocalDateTime startDate,
            LocalDateTime endDate,
            boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        static CouponResponse from(Coupon c) {
            return new CouponResponse(
                    c.getId(), c.getCode(), c.getDescription(), c.getDiscountType(),
                    c.getDiscountValue(), c.getMinOrderValue(), c.getMaxDiscountValue(),
                    c.getUsageLimit(), c.getUsedCount(), c.getStartDate(), c.getEndDate(),
                    c.isActive(), c.getCreatedAt(), c.getUpdatedAt()
            );
        }
    }

    record CreateCouponRequest(
            @NotBlank(message = "Mã coupon không được để trống") String code,
            String description,
            @NotNull(message = "Loại giảm giá không được để trống") DiscountType discountType,
            @NotNull(message = "Giá trị giảm không được để trống") @Positive BigDecimal discountValue,
            BigDecimal minOrderValue,
            BigDecimal maxDiscountValue,
            @Positive Integer usageLimit,
            LocalDateTime startDate,
            LocalDateTime endDate,
            boolean isActive
    ) {}

    record UpdateCouponRequest(
            String description,
            @NotNull(message = "Giá trị giảm không được để trống") @Positive BigDecimal discountValue,
            BigDecimal minOrderValue,
            BigDecimal maxDiscountValue,
            @Positive Integer usageLimit,
            LocalDateTime startDate,
            LocalDateTime endDate,
            boolean isActive
    ) {}

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
