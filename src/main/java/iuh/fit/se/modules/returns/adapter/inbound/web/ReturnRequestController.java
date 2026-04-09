package iuh.fit.se.modules.returns.adapter.inbound.web;

import iuh.fit.se.modules.returns.application.port.in.ReturnRequestUseCase;
import iuh.fit.se.modules.returns.domain.ItemCondition;
import iuh.fit.se.modules.returns.domain.ReturnRequest;
import iuh.fit.se.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReturnRequestController {

    private final ReturnRequestUseCase returnRequestUseCase;

    @PostMapping("/returns")
    @PreAuthorize("hasAuthority('RETURN_CREATE')")
    public ApiResponse<ReturnRequestResponseDTO> createReturn(@RequestBody CreateReturnRequestDTO dto) {
        // In real app, get customerId from SecurityContext
        Long customerId = 1L; 

        ReturnRequestUseCase.CreateReturnCommand command = ReturnRequestUseCase.CreateReturnCommand.builder()
                .orderId(dto.getOrderId())
                .customerId(customerId)
                .reason(dto.getReason())
                .notes(dto.getNotes())
                .items(dto.getItems().stream()
                        .map(item -> ReturnRequestUseCase.ReturnItemCommand.builder()
                                .bookId(item.getBookId())
                                .quantity(item.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        return ApiResponse.success(mapToDTO(returnRequestUseCase.createReturn(command)));
    }

    @GetMapping("/returns/my")
    @PreAuthorize("hasAuthority('RETURN_READ_OWN') or hasAuthority('RETURN_READ_SELF')")
    public ApiResponse<List<ReturnRequestResponseDTO>> getMyReturns() {
        Long customerId = 1L; // Get from SecurityContext
        return ApiResponse.success(returnRequestUseCase.getByCustomer(customerId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/admin/returns")
    @PreAuthorize("hasAuthority('RETURN_READ_ALL')")
    public ApiResponse<List<ReturnRequestResponseDTO>> getAllReturns() {
        return ApiResponse.success(returnRequestUseCase.getAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()));
    }

    @PutMapping("/admin/returns/{id}/approve")
    @PreAuthorize("hasAuthority('RETURN_APPROVE')")
    public ApiResponse<Void> approveReturn(@PathVariable String id) {
        String adminName = "ADMIN_USER"; // Get from SecurityContext
        returnRequestUseCase.approve(id, adminName);
        return ApiResponse.success(null);
    }

    @PutMapping("/admin/returns/{id}/receive")
    @PreAuthorize("hasAuthority('RETURN_RECEIVE')")
    public ApiResponse<Void> receiveReturn(@PathVariable String id, @RequestBody List<ItemCondition> conditions) {
        String warehouseStaff = "WAREHOUSE_USER"; // Get from SecurityContext
        returnRequestUseCase.markAsReceived(id, warehouseStaff, conditions);
        return ApiResponse.success(null);
    }

    @PutMapping("/admin/returns/{id}/refund")
    @PreAuthorize("hasAuthority('RETURN_APPROVE')") // Refund trigger by admin
    public ApiResponse<Void> refundReturn(@PathVariable String id) {
        String adminName = "ADMIN_USER";
        returnRequestUseCase.refund(id, adminName);
        return ApiResponse.success(null);
    }

    @PutMapping("/admin/returns/{id}/reject")
    @PreAuthorize("hasAuthority('RETURN_APPROVE')")
    public ApiResponse<Void> rejectReturn(@PathVariable String id, @RequestParam String reason) {
        String adminName = "ADMIN_USER";
        returnRequestUseCase.reject(id, reason, adminName);
        return ApiResponse.success(null);
    }

    private ReturnRequestResponseDTO mapToDTO(ReturnRequest request) {
        return ReturnRequestResponseDTO.builder()
                .id(request.getId())
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .status(request.getStatus())
                .refundAmount(request.getRefundAmount())
                .reason(request.getReason().name())
                .notes(request.getNotes())
                .createdAt(request.getCreatedAt())
                .items(request.getItems().stream()
                        .map(item -> ReturnRequestResponseDTO.ReturnItemResponseDTO.builder()
                                .id(item.getId())
                                .bookId(item.getBookId())
                                .quantity(item.getQuantity())
                                .refundPrice(item.getRefundPrice())
                                .condition(item.getCondition())
                                .build())
                        .collect(Collectors.toList()))
                .histories(request.getHistories().stream()
                        .map(h -> ReturnRequestResponseDTO.ReturnHistoryResponseDTO.builder()
                                .id(h.getId())
                                .fromStatus(h.getFromStatus())
                                .toStatus(h.getToStatus())
                                .changedBy(h.getChangedBy())
                                .changedAt(h.getChangedAt())
                                .note(h.getNote())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
