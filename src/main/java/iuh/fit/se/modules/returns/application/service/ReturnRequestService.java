package iuh.fit.se.modules.returns.application.service;

import iuh.fit.se.modules.returns.application.port.in.ReturnRequestUseCase;
import iuh.fit.se.modules.returns.application.port.out.OrderQueryPort;
import iuh.fit.se.modules.returns.application.port.out.ReturnRequestRepository;
import iuh.fit.se.modules.returns.domain.*;
import iuh.fit.se.shared.event.returns.ItemCondition;
import iuh.fit.se.modules.returns.domain.event.ReturnDomainEvents.*;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnRequestService implements ReturnRequestUseCase {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderQueryPort orderQueryPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ReturnRequest createReturn(CreateReturnCommand command) {
        log.info("Creating return request for order: {}", command.getOrderId());

        // 1. Get Order details from Order Module
        OrderQueryPort.OrderDto orderDto = orderQueryPort.findOrderById(command.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORD_NOT_FOUND));

        // 2. Business Validations
        if (!"COMPLETED".equals(orderDto.getStatus())) {
            throw new AppException(ErrorCode.RET_ORDER_NOT_DELIVERED);
        }

        if (!ReturnRequest.canCreateReturn(orderDto.getDeliveredAt(), 7)) {
            throw new AppException(ErrorCode.RET_EXCEEDED_RETURN_WINDOW);
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

        ReturnRequest saved = returnRequestRepository.save(returnRequest);

        // 4. Publish Domain Event (Internal)
        eventPublisher.publishEvent(ReturnRequestCreatedDomainEvent.of(saved));

        return saved;
    }

    @Override
    @Transactional
    public void approve(String returnRequestId, String approvedBy) {
        ReturnRequest request = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.RET_NOT_FOUND));

        request.approve(approvedBy);
        returnRequestRepository.save(request);

        eventPublisher.publishEvent(ReturnRequestApprovedDomainEvent.of(request));
    }

    @Override
    @Transactional
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
    }

    @Override
    @Transactional
    public void refund(String returnRequestId, String processedBy) {
        ReturnRequest request = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.RET_NOT_FOUND));

        BigDecimal totalRefund = request.getItems().stream()
                .map(item -> item.getRefundPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        request.refund(processedBy, totalRefund);
        returnRequestRepository.save(request);

        eventPublisher.publishEvent(ReturnRequestRefundedDomainEvent.of(request));
    }

    @Override
    @Transactional
    public void reject(String returnRequestId, String reason, String rejectedBy) {
        ReturnRequest request = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.RET_NOT_FOUND));

        request.reject(reason, rejectedBy);
        returnRequestRepository.save(request);

        eventPublisher.publishEvent(ReturnRequestRejectedDomainEvent.of(request, reason));
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
        return returnRequestRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnRequest> getByCustomer(Long customerId) {
        return returnRequestRepository.findByCustomerId(customerId);
    }
}
