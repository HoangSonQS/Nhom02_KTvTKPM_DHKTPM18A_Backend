package iuh.fit.se.modules.returns.application.service;

import iuh.fit.se.modules.returns.application.port.in.ReturnRequestUseCase;
import iuh.fit.se.modules.returns.application.port.out.OrderQueryPort;
import iuh.fit.se.modules.returns.application.port.out.ReturnEvidenceImagePort;
import iuh.fit.se.modules.returns.application.port.out.ReturnRequestRepository;
import iuh.fit.se.modules.returns.domain.*;
import iuh.fit.se.shared.event.returns.ItemCondition;
import iuh.fit.se.modules.returns.domain.event.ReturnDomainEvents.*;
import iuh.fit.se.shared.event.realtime.ReturnRealtimeEvent;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import iuh.fit.se.shared.infrastructure.cloudinary.CloudinaryUploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnRequestService implements ReturnRequestUseCase {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderQueryPort orderQueryPort;
    private final ReturnEvidenceImagePort returnEvidenceImagePort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ReturnRequest createReturn(CreateReturnCommand command) {
        log.info("Creating return request for order: {}", command.getOrderId());

        // 1. Get Order details from Order Module
        OrderQueryPort.OrderDto orderDto = orderQueryPort.findOrderById(command.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORD_NOT_FOUND));

        // 2. Business Validations
        if (!Objects.equals(orderDto.getCustomerId(), command.getCustomerId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // FulfillmentStatus.DELIVERED is the eligibility gate for returns (replaces legacy COMPLETED)
        if (!"DELIVERED".equals(orderDto.getStatus())) {
            throw new AppException(ErrorCode.RET_ORDER_NOT_DELIVERED);
        }

        if (!ReturnRequest.canCreateReturn(orderDto.getDeliveredAt(), 7)) {
            throw new AppException(ErrorCode.RET_EXCEEDED_RETURN_WINDOW);
        }

        // 2a. Check if return request already exists
        List<ReturnRequest> existingRequests = returnRequestRepository.findByOrderId(command.getOrderId());
        boolean hasActiveReturn = existingRequests.stream()
                .anyMatch(r -> r.getStatus() != ReturnStatus.REJECTED);
        if (hasActiveReturn) {
            throw new AppException(ErrorCode.RET_ALREADY_EXISTS);
        }

        // Check if items match order items
        List<ReturnItem> items = command.getItems().stream()
                .map(itemCmd -> {
                    OrderQueryPort.OrderItemDto orderItem = orderDto.getItems().stream()
                            .filter(oi -> oi.getBookId().equals(itemCmd.getBookId()))
                            .findFirst()
                            .orElseThrow(() -> new AppException(ErrorCode.RET_INVALID_ITEMS, "Sách không có trong đơn hàng: " + itemCmd.getBookId()));

                    if (itemCmd.getQuantity() > orderItem.getQuantity()) {
                        throw new AppException(ErrorCode.RET_INVALID_ITEMS, "Số lượng trả vượt quá số lượng đã mua");
                    }

                    return ReturnItem.create(
                            UUID.randomUUID().toString(),
                            itemCmd.getBookId(),
                            itemCmd.getQuantity(),
                            orderItem.getPrice(),
                            ItemCondition.GOOD // Default condition, will be verified later at warehouse
                    );
                })
                .collect(Collectors.toList());

        // 3. Create and Save ReturnRequest
        ReturnRequest returnRequest = ReturnRequest.create(
                command.getOrderId(),
                command.getCustomerId(),
                command.getReason(),
                command.getNotes(),
                items
        );

        if (command.getEvidenceImageFile() != null && command.getEvidenceImageFile().length > 0) {
            CloudinaryUploadResult uploadResult = returnEvidenceImagePort.uploadEvidenceImage(command.getEvidenceImageFile());
            returnRequest.attachEvidenceImage(uploadResult.url(), uploadResult.publicId());
        }

        ReturnRequest saved = returnRequestRepository.save(returnRequest);

        // 4. Publish Domain Event (Internal)
        eventPublisher.publishEvent(ReturnRequestCreatedDomainEvent.of(saved));
        eventPublisher.publishEvent(ReturnRealtimeEvent.created(
                saved.getId(),
                saved.getOrderId(),
                saved.getCustomerId()
        ));

        return saved;
    }

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "STAFF_APPROVE_RETURN")
    public void approve(String returnRequestId, String approvedBy) {
        ReturnRequest request = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.RET_NOT_FOUND));

        request.approve(approvedBy);
        returnRequestRepository.save(request);

        eventPublisher.publishEvent(ReturnRequestApprovedDomainEvent.of(request));
        publishReturnStatusChanged(request);
    }

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "STAFF_RECEIVE_RETURN")
    public void markAsReceived(String returnRequestId, String receivedBy, List<ItemCondition> conditions) {
        ReturnRequest request = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.RET_NOT_FOUND));

        if (conditions.size() != request.getItems().size()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Số lượng đánh giá tình trạng hàng không khớp");
        }

        request.markAsReceived(receivedBy, conditions);
        
        // Manual update of conditions via reflect for legacy consistency
        for (int i = 0; i < request.getItems().size(); i++) {
            try {
                java.lang.reflect.Field field = ReturnItem.class.getDeclaredField("condition");
                field.setAccessible(true);
                field.set(request.getItems().get(i), conditions.get(i));
            } catch (Exception e) {
                log.error("Failed to update item condition", e);
            }
        }
        
        returnRequestRepository.save(request);
        eventPublisher.publishEvent(ReturnRequestReceivedDomainEvent.of(request));
        publishReturnStatusChanged(request);
    }

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "STAFF_REFUND_RETURN")
    public void refund(String returnRequestId, String processedBy) {
        ReturnRequest request = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.RET_NOT_FOUND));

        BigDecimal totalRefund = request.getItems().stream()
                .map(item -> item.getRefundPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        request.refund(processedBy, totalRefund);
        returnRequestRepository.save(request);

        eventPublisher.publishEvent(ReturnRequestRefundedDomainEvent.of(request));
        publishReturnStatusChanged(request);
    }

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "STAFF_REJECT_RETURN")
    public void reject(String returnRequestId, String reason, String rejectedBy) {
        ReturnRequest request = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.RET_NOT_FOUND));

        request.reject(reason, rejectedBy);
        returnRequestRepository.save(request);

        eventPublisher.publishEvent(ReturnRequestRejectedDomainEvent.of(request, reason));
        publishReturnStatusChanged(request);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnRequest getById(String id) {
        return returnRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RET_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnRequest> getAll() {
        return returnRequestRepository.findAllNewestFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnRequest> getByCustomer(Long customerId) {
        return returnRequestRepository.findByCustomerIdNewestFirst(customerId);
    }

    private void publishReturnStatusChanged(ReturnRequest request) {
        eventPublisher.publishEvent(ReturnRealtimeEvent.statusChanged(
                request.getId(),
                request.getOrderId(),
                request.getCustomerId(),
                request.getStatus().name()
        ));
    }
}
