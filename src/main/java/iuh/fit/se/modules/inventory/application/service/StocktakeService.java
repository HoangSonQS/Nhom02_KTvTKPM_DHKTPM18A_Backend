package iuh.fit.se.modules.inventory.application.service;

import iuh.fit.se.modules.inventory.application.port.in.StocktakeUseCase;
import iuh.fit.se.modules.inventory.application.port.out.InventoryPersistencePort;
import iuh.fit.se.modules.inventory.application.port.out.StocktakePersistencePort;
import iuh.fit.se.modules.inventory.domain.InventoryStock;
import iuh.fit.se.modules.inventory.domain.StockHistory;
import iuh.fit.se.modules.inventory.domain.StockHistoryStatus;
import iuh.fit.se.modules.inventory.domain.StocktakeItem;
import iuh.fit.se.modules.inventory.domain.StocktakeSession;
import iuh.fit.se.modules.inventory.domain.StocktakeStatus;
import iuh.fit.se.shared.event.inventory.InventoryStockChangedIntegrationEvent;
import iuh.fit.se.shared.event.realtime.AdminDataChangedRealtimeEvent;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StocktakeService implements StocktakeUseCase {

    private static final String STOCKTAKE_SOURCE = "STOCKTAKE";

    private final StocktakePersistencePort stocktakePort;
    private final InventoryPersistencePort inventoryPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "ADMIN_CREATE_STOCKTAKE")
    public StocktakeResponse create(CreateStocktakeCommand command, String createdBy) {
        List<Long> bookIds = normalizeBookIds(command.bookIds());
        Map<Long, InventoryStock> stocks = inventoryPort.findStocksByBookIds(bookIds).stream()
                .collect(Collectors.toMap(InventoryStock::getBookId, Function.identity()));

        List<StocktakeItem> items = bookIds.stream()
                .map(bookId -> StocktakeItem.snapshot(bookId, requireStock(bookId, stocks).getQuantity()))
                .toList();

        StocktakeSession session = StocktakeSession.create(
                command.name(),
                createdBy,
                command.assignedStaffId(),
                command.assignedStaffEmail(),
                items
        );
        StocktakeSession saved = stocktakePort.save(session);
        publishStocktakeChanged("Phiên kiểm kê đã được tạo");
        return toResponse(saved);
    }

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "ADMIN_ASSIGN_STOCKTAKE")
    public StocktakeResponse assign(Long id, AssignStocktakeCommand command) {
        StocktakeSession session = load(id);
        session.assignStaff(command.assignedStaffId(), command.assignedStaffEmail());
        StocktakeSession saved = stocktakePort.save(session);
        publishStocktakeChanged("Phiên kiểm kê đã được giao");
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StocktakeSummaryResponse> listForCurrentUser(Long userId, String role) {
        List<StocktakeSession> sessions = isAdmin(role)
                ? stocktakePort.findAll()
                : stocktakePort.findByAssignedStaffId(userId);
        return sessions.stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StocktakeResponse getForCurrentUser(Long id, Long userId, String role) {
        StocktakeSession session = load(id);
        ensureCanAccess(session, userId, role);
        return toResponse(session);
    }

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "STAFF_UPDATE_STOCKTAKE_ACTUALS")
    public StocktakeResponse updateActualQuantities(Long id, UpdateActualQuantitiesCommand command, Long userId, String role) {
        StocktakeSession session = load(id);
        ensureCanEditActuals(session, userId, role);
        if (session.getStatus() != StocktakeStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Chỉ có thể nhập kết quả khi phiên đang kiểm kê");
        }
        if (command.items() == null || command.items().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Vui lòng nhập kết quả kiểm kê");
        }
        Map<Long, ActualQuantityItem> byBookId = command.items().stream()
                .collect(Collectors.toMap(ActualQuantityItem::bookId, Function.identity(), (first, second) -> second));

        session.getItems().forEach(item -> {
            ActualQuantityItem actual = byBookId.get(item.getBookId());
            if (actual != null) {
                item.recordActualQuantity(actual.actualQuantity(), actual.note());
            }
        });

        StocktakeSession saved = stocktakePort.save(session);
        publishStocktakeChanged("Kết quả kiểm kê đã được cập nhật");
        return toResponse(saved);
    }

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "STAFF_SUBMIT_STOCKTAKE")
    public StocktakeResponse submit(Long id, Long userId, String email, String role) {
        StocktakeSession session = load(id);
        ensureCanEditActuals(session, userId, role);
        session.submit(email);
        StocktakeSession saved = stocktakePort.save(session);
        publishStocktakeChanged("Báo cáo kiểm kê đã được gửi");
        return toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "inventory_stock_cache", allEntries = true),
            @CacheEvict(value = "bookDetails", allEntries = true),
            @CacheEvict(value = "books", allEntries = true)
    })
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "ADMIN_APPROVE_STOCKTAKE")
    public StocktakeResponse approve(Long id, String approvedBy) {
        StocktakeSession session = load(id);
        if (session.getStatus() != StocktakeStatus.SUBMITTED) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Chỉ có thể duyệt báo cáo đang chờ duyệt");
        }

        session.getItems().forEach(item -> applyApprovedStocktake(session.getId(), item));
        session.approve(approvedBy);
        StocktakeSession saved = stocktakePort.save(session);
        publishStocktakeChanged("Báo cáo kiểm kê đã được duyệt");
        return toResponse(saved);
    }

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "ADMIN_REJECT_STOCKTAKE")
    public StocktakeResponse reject(Long id, RejectStocktakeCommand command, String rejectedBy) {
        StocktakeSession session = load(id);
        session.reject(rejectedBy, command.reason());
        StocktakeSession saved = stocktakePort.save(session);
        publishStocktakeChanged("Báo cáo kiểm kê đã bị từ chối");
        return toResponse(saved);
    }

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "ADMIN_CANCEL_STOCKTAKE")
    public StocktakeResponse cancel(Long id, String cancelledBy) {
        StocktakeSession session = load(id);
        session.cancel(cancelledBy);
        StocktakeSession saved = stocktakePort.save(session);
        publishStocktakeChanged("Phiên kiểm kê đã bị hủy");
        return toResponse(saved);
    }

    private void applyApprovedStocktake(Long sessionId, StocktakeItem item) {
        if (item.getActualQuantity() == null) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Báo cáo kiểm kê còn thiếu số lượng thực tế");
        }
        InventoryStock currentStock = inventoryPort.findStockByBookId(item.getBookId())
                .orElseThrow(() -> new AppException(ErrorCode.INV_STOCK_NOT_FOUND));
        int adjustmentFromCurrent = item.getActualQuantity() - currentStock.getQuantity();
        int updated = inventoryPort.setStockQuantity(item.getBookId(), item.getActualQuantity());
        if (updated == 0) {
            throw new AppException(ErrorCode.INV_STOCK_LOCK_TIMEOUT);
        }

        StockHistory history = StockHistory.builder()
                .referenceId("STOCKTAKE_" + sessionId + "_" + item.getBookId())
                .bookId(item.getBookId())
                .amount(adjustmentFromCurrent)
                .type("STOCKTAKE_ADJUSTMENT")
                .status(StockHistoryStatus.PENDING)
                .requestData(toJson(
                        "systemQuantity", item.getSystemQuantity(),
                        "actualQuantity", item.getActualQuantity(),
                        "difference", item.getDifference()
                ))
                .build();
        history.markSuccess(toJson(
                "previousQuantity", currentStock.getQuantity(),
                "finalQuantity", item.getActualQuantity(),
                "adjustment", adjustmentFromCurrent
        ));
        inventoryPort.saveHistory(history);
        eventPublisher.publishEvent(InventoryStockChangedIntegrationEvent.of(
                item.getBookId(),
                adjustmentFromCurrent,
                item.getActualQuantity(),
                adjustmentFromCurrent >= 0 ? "INCREASE" : "DECREASE"
        ));
    }

    private StocktakeSession load(Long id) {
        return stocktakePort.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy phiên kiểm kê"));
    }

    private InventoryStock requireStock(Long bookId, Map<Long, InventoryStock> stocks) {
        InventoryStock stock = stocks.get(bookId);
        if (stock == null) {
            throw new AppException(ErrorCode.INV_STOCK_NOT_FOUND, "Không tìm thấy tồn kho cho sách " + bookId);
        }
        return stock;
    }

    private List<Long> normalizeBookIds(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Vui lòng chọn ít nhất một sách để kiểm kê");
        }
        return new LinkedHashSet<>(bookIds).stream().toList();
    }

    private void ensureCanAccess(StocktakeSession session, Long userId, String role) {
        if (!isAdmin(role) && !session.isAssignedTo(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void ensureCanEditActuals(StocktakeSession session, Long userId, String role) {
        if (isAdmin(role)) {
            return;
        }
        if (!isStocktakeStaff(role) || !session.isAssignedTo(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equals(role);
    }

    private boolean isStocktakeStaff(String role) {
        return "STAFF".equals(role) || "STAFF_WAREHOUSE".equals(role) || "WAREHOUSE_STAFF".equals(role);
    }

    private StocktakeSummaryResponse toSummary(StocktakeSession session) {
        int differenceCount = (int) session.getItems().stream()
                .filter(item -> item.getDifference() != null && item.getDifference() != 0)
                .count();
        return StocktakeSummaryResponse.builder()
                .id(session.getId())
                .name(session.getName())
                .status(session.getStatus())
                .statusLabel(statusLabel(session.getStatus()))
                .assignedStaffId(session.getAssignedStaffId())
                .assignedStaffEmail(session.getAssignedStaffEmail())
                .createdBy(session.getCreatedBy())
                .createdAt(session.getCreatedAt())
                .submittedAt(session.getSubmittedAt())
                .approvedAt(session.getApprovedAt())
                .itemCount(session.getItems().size())
                .differenceCount(differenceCount)
                .build();
    }

    private StocktakeResponse toResponse(StocktakeSession session) {
        return StocktakeResponse.builder()
                .id(session.getId())
                .name(session.getName())
                .status(session.getStatus())
                .statusLabel(statusLabel(session.getStatus()))
                .assignedStaffId(session.getAssignedStaffId())
                .assignedStaffEmail(session.getAssignedStaffEmail())
                .createdBy(session.getCreatedBy())
                .createdAt(session.getCreatedAt())
                .submittedBy(session.getSubmittedBy())
                .submittedAt(session.getSubmittedAt())
                .approvedBy(session.getApprovedBy())
                .approvedAt(session.getApprovedAt())
                .rejectedBy(session.getRejectedBy())
                .rejectedAt(session.getRejectedAt())
                .rejectReason(session.getRejectReason())
                .items(session.getItems().stream().map(this::toItemResponse).toList())
                .build();
    }

    private StocktakeItemResponse toItemResponse(StocktakeItem item) {
        return StocktakeItemResponse.builder()
                .id(item.getId())
                .bookId(item.getBookId())
                .systemQuantity(item.getSystemQuantity())
                .actualQuantity(item.getActualQuantity())
                .difference(item.getDifference())
                .note(item.getNote())
                .build();
    }

    private String statusLabel(StocktakeStatus status) {
        return switch (status) {
            case IN_PROGRESS -> "Đang kiểm kê";
            case SUBMITTED -> "Chờ duyệt";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Đã từ chối";
            case CANCELLED -> "Đã hủy";
        };
    }

    private void publishStocktakeChanged(String message) {
        eventPublisher.publishEvent(AdminDataChangedRealtimeEvent.of(STOCKTAKE_SOURCE, message));
    }

    private String toJson(Object... entries) {
        StringBuilder builder = new StringBuilder("{");
        for (int i = 0; i < entries.length; i += 2) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"').append(entries[i]).append("\":").append(entries[i + 1]);
        }
        return builder.append('}').toString();
    }
}
