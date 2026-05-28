package iuh.fit.se.modules.inventory.application.port.in;

import iuh.fit.se.modules.inventory.domain.StocktakeStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public interface StocktakeUseCase {
    StocktakeResponse create(CreateStocktakeCommand command, String createdBy);
    StocktakeResponse assign(Long id, AssignStocktakeCommand command);
    List<StocktakeSummaryResponse> listForCurrentUser(Long userId, String role);
    StocktakeResponse getForCurrentUser(Long id, Long userId, String role);
    StocktakeResponse updateActualQuantities(Long id, UpdateActualQuantitiesCommand command, Long userId, String role);
    StocktakeResponse submit(Long id, Long userId, String email, String role);
    StocktakeResponse approve(Long id, String approvedBy);
    StocktakeResponse reject(Long id, RejectStocktakeCommand command, String rejectedBy);
    StocktakeResponse cancel(Long id, String cancelledBy);

    record CreateStocktakeCommand(String name, Long assignedStaffId, String assignedStaffEmail, List<Long> bookIds) {}
    record AssignStocktakeCommand(Long assignedStaffId, String assignedStaffEmail) {}
    record UpdateActualQuantitiesCommand(List<ActualQuantityItem> items) {}
    record ActualQuantityItem(Long bookId, int actualQuantity, String note) {}
    record RejectStocktakeCommand(String reason) {}

    @Builder
    record StocktakeSummaryResponse(
            Long id,
            String name,
            StocktakeStatus status,
            String statusLabel,
            Long assignedStaffId,
            String assignedStaffEmail,
            String createdBy,
            LocalDateTime createdAt,
            LocalDateTime submittedAt,
            LocalDateTime approvedAt,
            int itemCount,
            int differenceCount
    ) {}

    @Builder
    record StocktakeResponse(
            Long id,
            String name,
            StocktakeStatus status,
            String statusLabel,
            Long assignedStaffId,
            String assignedStaffEmail,
            String createdBy,
            LocalDateTime createdAt,
            String submittedBy,
            LocalDateTime submittedAt,
            String approvedBy,
            LocalDateTime approvedAt,
            String rejectedBy,
            LocalDateTime rejectedAt,
            String rejectReason,
            List<StocktakeItemResponse> items
    ) {}

    @Builder
    record StocktakeItemResponse(
            Long id,
            Long bookId,
            int systemQuantity,
            Integer actualQuantity,
            Integer difference,
            String note
    ) {}
}
