package iuh.fit.se.modules.promotion.adapter.inbound.web;

import iuh.fit.se.modules.promotion.application.port.in.FlashSaleUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleUseCase flashSaleUseCase;

    @GetMapping("/api/v1/flash-sales/active")
    public ResponseEntity<ApiResponse<FlashSaleUseCase.ActiveFlashSaleResponse>> getActive() {
        return ResponseEntity.ok(ApiResponse.success(flashSaleUseCase.getActive()));
    }

    @GetMapping("/api/v1/admin/flash-sales")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<FlashSaleUseCase.FlashSaleResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(flashSaleUseCase.getAll()));
    }

    @PostMapping("/api/v1/admin/flash-sales")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FlashSaleUseCase.FlashSaleResponse>> create(@Valid @RequestBody FlashSaleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tao flash sale thanh cong", flashSaleUseCase.create(toCommand(request))));
    }

    @PutMapping("/api/v1/admin/flash-sales/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FlashSaleUseCase.FlashSaleResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody FlashSaleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Cap nhat flash sale thanh cong", flashSaleUseCase.update(id, toCommand(request))));
    }

    @DeleteMapping("/api/v1/admin/flash-sales/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        flashSaleUseCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xoa flash sale thanh cong", null));
    }

    private FlashSaleUseCase.FlashSaleCommand toCommand(FlashSaleRequest request) {
        return new FlashSaleUseCase.FlashSaleCommand(
                request.bookId(),
                request.saleQuantity(),
                request.discountPercent(),
                request.startAt(),
                request.endAt(),
                request.active()
        );
    }

    public record FlashSaleRequest(
            @NotNull Long bookId,
            @Min(1) int saleQuantity,
            @Min(1) @Max(90) int discountPercent,
            @NotNull LocalDateTime startAt,
            @NotNull LocalDateTime endAt,
            boolean active
    ) {}
}
