package iuh.fit.se.modules.promotion.adapter.inbound.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.modules.promotion.application.port.in.PromotionAdminUseCase;
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

@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
@Tag(name = "Promotion Admin", description = "APIs quản lý mã khuyến mãi dành cho Admin")
@PreAuthorize("hasRole('ADMIN')")
public class PromotionAdminController {

    private final PromotionAdminUseCase adminUseCase;

    @GetMapping
    @Operation(summary = "Lấy toàn bộ danh sách coupon")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getAllCoupons() {
        List<CouponResponse> coupons = adminUseCase.getAllCoupons()
                .stream()
                .map(CouponResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Danh sách mã khuyến mãi", coupons));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết một coupon theo ID")
    public ResponseEntity<ApiResponse<CouponResponse>> getCouponById(@PathVariable Long id) {
        Coupon coupon = adminUseCase.getCouponById(id);
        return ResponseEntity.ok(ApiResponse.success("Chi tiết mã khuyến mãi", CouponResponse.from(coupon)));
    }

    @PostMapping
    @Operation(summary = "Tạo mới một coupon")
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

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin coupon")
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

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa một coupon")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable Long id) {
        adminUseCase.deleteCoupon(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa mã khuyến mãi thành công", null));
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    public record CouponResponse(
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
        public static CouponResponse from(Coupon c) {
            return new CouponResponse(
                    c.getId(), c.getCode(), c.getDescription(), c.getDiscountType(),
                    c.getDiscountValue(), c.getMinOrderValue(), c.getMaxDiscountValue(),
                    c.getUsageLimit(), c.getUsedCount(), c.getStartDate(), c.getEndDate(),
                    c.isActive(), c.getCreatedAt(), c.getUpdatedAt()
            );
        }
    }

    public record CreateCouponRequest(
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

    public record UpdateCouponRequest(
            String description,
            @NotNull(message = "Giá trị giảm không được để trống") @Positive BigDecimal discountValue,
            BigDecimal minOrderValue,
            BigDecimal maxDiscountValue,
            @Positive Integer usageLimit,
            LocalDateTime startDate,
            LocalDateTime endDate,
            boolean isActive
    ) {}
}
