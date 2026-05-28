package iuh.fit.se.modules.inventory.adapter.inbound.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.modules.inventory.application.port.in.StocktakeUseCase;
import iuh.fit.se.modules.inventory.application.port.in.StocktakeUseCase.AssignStocktakeCommand;
import iuh.fit.se.modules.inventory.application.port.in.StocktakeUseCase.CreateStocktakeCommand;
import iuh.fit.se.modules.inventory.application.port.in.StocktakeUseCase.RejectStocktakeCommand;
import iuh.fit.se.modules.inventory.application.port.in.StocktakeUseCase.StocktakeResponse;
import iuh.fit.se.modules.inventory.application.port.in.StocktakeUseCase.StocktakeSummaryResponse;
import iuh.fit.se.modules.inventory.application.port.in.StocktakeUseCase.UpdateActualQuantitiesCommand;
import iuh.fit.se.shared.api.ApiResponse;
import iuh.fit.se.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/stocktakes")
@RequiredArgsConstructor
@Tag(name = "Inventory Stocktake", description = "APIs kiểm kê kho")
@PreAuthorize("hasRole('ADMIN') or hasRole('STAFF_WAREHOUSE') or hasAuthority('INVENTORY_READ')")
public class InventoryStocktakeController {

    private final StocktakeUseCase stocktakeUseCase;

    @GetMapping
    @Operation(summary = "Lấy danh sách phiên kiểm kê")
    public ResponseEntity<ApiResponse<List<StocktakeSummaryResponse>>> list() {
        List<StocktakeSummaryResponse> sessions = stocktakeUseCase.listForCurrentUser(
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentRole()
        );
        return ResponseEntity.ok(ApiResponse.success("Danh sách phiên kiểm kê", sessions));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết phiên kiểm kê")
    public ResponseEntity<ApiResponse<StocktakeResponse>> get(@PathVariable Long id) {
        StocktakeResponse session = stocktakeUseCase.getForCurrentUser(
                id,
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentRole()
        );
        return ResponseEntity.ok(ApiResponse.success("Chi tiết phiên kiểm kê", session));
    }

    @PostMapping
    @Operation(summary = "Tạo phiên kiểm kê")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StocktakeResponse>> create(@RequestBody CreateStocktakeCommand command) {
        StocktakeResponse created = stocktakeUseCase.create(command, SecurityUtils.getCurrentEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo phiên kiểm kê thành công", created));
    }

    @PutMapping("/{id}/assign")
    @Operation(summary = "Giao phiên kiểm kê cho nhân viên")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StocktakeResponse>> assign(
            @PathVariable Long id,
            @RequestBody AssignStocktakeCommand command
    ) {
        StocktakeResponse session = stocktakeUseCase.assign(id, command);
        return ResponseEntity.ok(ApiResponse.success("Giao phiên kiểm kê thành công", session));
    }

    @PutMapping("/{id}/actuals")
    @Operation(summary = "Nhập số lượng thực tế")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF_WAREHOUSE') or hasAuthority('INVENTORY_IMPORT_STOCK')")
    public ResponseEntity<ApiResponse<StocktakeResponse>> updateActuals(
            @PathVariable Long id,
            @RequestBody UpdateActualQuantitiesCommand command
    ) {
        StocktakeResponse session = stocktakeUseCase.updateActualQuantities(
                id,
                command,
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentRole()
        );
        return ResponseEntity.ok(ApiResponse.success("Cập nhật kết quả kiểm kê thành công", session));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Gửi báo cáo kiểm kê")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF_WAREHOUSE') or hasAuthority('INVENTORY_IMPORT_STOCK')")
    public ResponseEntity<ApiResponse<StocktakeResponse>> submit(@PathVariable Long id) {
        StocktakeResponse session = stocktakeUseCase.submit(
                id,
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentEmail(),
                SecurityUtils.getCurrentRole()
        );
        return ResponseEntity.ok(ApiResponse.success("Gửi báo cáo kiểm kê thành công", session));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Duyệt báo cáo kiểm kê")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StocktakeResponse>> approve(@PathVariable Long id) {
        StocktakeResponse session = stocktakeUseCase.approve(id, SecurityUtils.getCurrentEmail());
        return ResponseEntity.ok(ApiResponse.success("Duyệt báo cáo kiểm kê thành công", session));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Từ chối báo cáo kiểm kê")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StocktakeResponse>> reject(
            @PathVariable Long id,
            @RequestBody RejectStocktakeCommand command
    ) {
        StocktakeResponse session = stocktakeUseCase.reject(id, command, SecurityUtils.getCurrentEmail());
        return ResponseEntity.ok(ApiResponse.success("Từ chối báo cáo kiểm kê thành công", session));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Hủy phiên kiểm kê")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StocktakeResponse>> cancel(@PathVariable Long id) {
        StocktakeResponse session = stocktakeUseCase.cancel(id, SecurityUtils.getCurrentEmail());
        return ResponseEntity.ok(ApiResponse.success("Hủy phiên kiểm kê thành công", session));
    }
}
