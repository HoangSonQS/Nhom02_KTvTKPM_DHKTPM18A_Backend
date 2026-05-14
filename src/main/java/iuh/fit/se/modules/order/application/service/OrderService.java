package iuh.fit.se.modules.order.application.service;

import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase;
import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase.UpdateFulfillmentStatusCommand;
import iuh.fit.se.modules.order.application.port.out.CartPort;
import iuh.fit.se.modules.order.application.port.out.InventoryPort;
import iuh.fit.se.modules.order.application.port.out.OrderPersistencePort;
import iuh.fit.se.modules.order.application.port.out.OrderUserPort;
import iuh.fit.se.modules.order.application.port.out.PromotionPort;
import iuh.fit.se.modules.order.domain.*;
import iuh.fit.se.modules.order.domain.event.OrderCreatedDomainEvent;
import iuh.fit.se.modules.order.domain.event.OrderFulfillmentStatusChangedEvent;
import iuh.fit.se.modules.order.domain.exception.InvalidOrderTransitionException;
import iuh.fit.se.shared.audit.application.port.out.AuditEventPublisherPort;
import iuh.fit.se.shared.audit.domain.event.UserActionAuditedEvent;
import iuh.fit.se.shared.config.UserPrincipal;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService implements OrderInternalUseCase {

    private final OrderPersistencePort orderPersistencePort;
    private final CartPort cartPort;
    private final InventoryPort inventoryPort;
    private final PromotionPort promotionPort;
    private final OrderUserPort orderUserPort;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditEventPublisherPort auditEventPublisherPort;

    @Override
    public OrderResponse checkout(Long userId, CheckoutCommand command) {
        MDC.put("requestId", command.getRequestId());
        try {
            log.info("Starting checkout saga for user {} with ref {}", userId, command.getRequestId());

            // 1. Idempotency check
            Optional<Order> existing = orderPersistencePort.findByRequestId(command.getRequestId());
            Order order;
            if (existing.isPresent()) {
                order = existing.get();
                if (order.getSagaStatus() == SagaStatus.COMPLETED) {
                    log.info("Order with requestId {} already completed. Returning existing response.", command.getRequestId());
                    cartPort.clearCart(userId);
                    return mapToResponse(order);
                } else if (order.getSagaStatus() == SagaStatus.FAILED) {
                    log.info("Order with requestId {} was FAILED. Resetting for retry.", command.getRequestId());
                    order.resetForRetry();
                    orderPersistencePort.save(order);
                } else {
                    log.warn("Order with requestId {} exists in status {}. Returning current state.",
                            command.getRequestId(), order.getSagaStatus());
                    return mapToResponse(order);
                }
            } else {
                order = null; // Will create below
            }

            // 2. Fetch User Profile để fallback địa chỉ & SĐT
            OrderUserPort.UserDto userProfile = orderUserPort.getUserDetails(userId);
            String shippingAddress = (command.getShippingAddress() == null || command.getShippingAddress().isBlank())
                    ? userProfile.getDefaultAddress()
                    : command.getShippingAddress();
            String customerPhone = (command.getCustomerPhone() == null || command.getCustomerPhone().isBlank())
                    ? userProfile.getPhoneNumber()
                    : command.getCustomerPhone();

            if (shippingAddress == null || shippingAddress.isBlank()) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Vui lòng cung cấp địa chỉ giao hàng hoặc thiết lập địa chỉ mặc định trong hồ sơ.");
            }
            if (customerPhone == null || customerPhone.isBlank()) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Vui lòng cung cấp số điện thoại hoặc cập nhật số điện thoại trong hồ sơ.");
            }

            // Tạo resolved command với thông tin đã được resolve từ profile
            CheckoutCommand resolvedCommand = CheckoutCommand.builder()
                    .requestId(command.getRequestId())
                    .shippingAddress(shippingAddress)
                    .customerPhone(customerPhone)
                    .couponCode(command.getCouponCode())
                    .paymentMethod(command.getPaymentMethod())
                    .build();

            // 3. Fetch Cart
            CartPort.CartDto cart = cartPort.getCartByUserId(userId);
            if (cart.getItems() == null || cart.getItems().isEmpty()) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Giỏ hàng rỗng, không thể tạo đơn hàng.");
            }

            // 4. INIT Order in DB if not exists
            BigDecimal totalAmount = cart.getItems().stream()
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (order == null) {
                order = saveInitialOrder(userId, resolvedCommand, cart, totalAmount);
            } else {
                // If retrying, update amounts and items to match current cart
                order = updateOrderFromCart(order, resolvedCommand, cart, totalAmount);
            }
            
            final Order sagaOrder = order;
            
            // Extract stock items for compensation/inventory phases
            List<InventoryPort.StockItem> stockItems = cart.getItems().stream()
                    .map(item -> InventoryPort.StockItem.builder()
                            .bookId(item.getBookId())
                                    .quantity(item.getQuantity())
                                    .build())
                            .collect(Collectors.toList());

            // 5. Inventory Phase (Decrease)
            try {
                inventoryPort.decreaseStockBulk(stockItems, resolvedCommand.getRequestId());
                updateSagaStatus(sagaOrder, SagaStatus.STOCK_RESERVED);
            } catch (Exception e) {
                log.error("Inventory Phase failed for requestId {}: {}", resolvedCommand.getRequestId(), e.getMessage());
                updateSagaStatus(sagaOrder, SagaStatus.FAILED);
                throw e;
            }

            // 6. Promotion Phase
            BigDecimal discount = BigDecimal.ZERO;
            if (resolvedCommand.getCouponCode() != null && !resolvedCommand.getCouponCode().isBlank()) {
                PromotionPort.PromotionResult promoResult;
                try {
                    promoResult = promotionPort.reserveCoupon(
                            resolvedCommand.getCouponCode(), totalAmount, resolvedCommand.getRequestId());
                } catch (AppException e) {
                    log.error("Promotion business error: {}. Compensating stock...", e.getMessage());
                    inventoryPort.increaseStockBulk(stockItems, resolvedCommand.getRequestId());
                    updateSagaStatus(sagaOrder, SagaStatus.FAILED);
                    throw e;
                } catch (Exception e) {
                    log.error("Promotion Phase network/system error. Compensating stock...");
                    inventoryPort.increaseStockBulk(stockItems, resolvedCommand.getRequestId());
                    updateSagaStatus(sagaOrder, SagaStatus.FAILED);
                    throw new AppException(ErrorCode.INTERNAL_ERROR, "Lỗi hệ thống khi xử lý mã giảm giá.");
                }

                if (promoResult.isSuccess()) {
                    discount = promoResult.getDiscountAmount();
                    updateOrderWithPromotion(sagaOrder, discount, SagaStatus.COUPON_RESERVED);
                } else {
                    log.warn("Promotion business failed: {}. Executing compensation for Stock...", promoResult.getMessage());
                    inventoryPort.increaseStockBulk(stockItems, resolvedCommand.getRequestId());
                    updateSagaStatus(sagaOrder, SagaStatus.FAILED);
                    throw new AppException(ErrorCode.INVALID_INPUT, "Mã giảm giá không hợp lệ: " + promoResult.getMessage());
                }
            }

            // 7. Complete Phase
            completeSaga(sagaOrder);
            cartPort.clearCart(userId);

            // 8. Re-fetch order to get updated discount and saga status from DB
            Order finalOrder = orderPersistencePort.findById(sagaOrder.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.ORD_NOT_FOUND));
            if (isCashOnDelivery(resolvedCommand)) {
                finalOrder.confirm();
                if (resolvedCommand.getCouponCode() != null && !resolvedCommand.getCouponCode().isBlank()) {
                    promotionPort.confirmCouponUsage(resolvedCommand.getRequestId());
                }
                finalOrder = orderPersistencePort.save(finalOrder);
            }

            // 9. Publish Domain Event (Internal)
            eventPublisher.publishEvent(OrderCreatedDomainEvent.of(finalOrder, userProfile.getFullName(), userProfile.getEmail()));

            return mapToResponse(finalOrder);

        } finally {
            MDC.remove("requestId");
        }
    }

    private boolean isCashOnDelivery(CheckoutCommand command) {
        return "COD".equalsIgnoreCase(command.getPaymentMethod())
                || "CASH".equalsIgnoreCase(command.getPaymentMethod());
    }

    private OrderResponse mapToResponse(Order order) {
        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        return OrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .discountAmount(discount)
                .finalAmount(order.getTotalAmount().subtract(discount))
                .fulfillmentStatus(order.getFulfillmentStatus().name())
                .sagaStatus(order.getSagaStatus().name())
                .requestId(order.getRequestId())
                .updatedAt(order.getUpdatedAt())
                .items(order.getItems().stream()
                        .map(item -> OrderItemResponse.builder()
                                .bookId(item.getBookId())
                                .title(item.getBookTitle())
                                .quantity(item.getQuantity())
                                .priceAtPurchase(item.getPriceAtPurchase())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Transactional
    public Order updateOrderFromCart(Order order, CheckoutCommand command, CartPort.CartDto cart, BigDecimal totalAmount) {
        Order o = orderPersistencePort.findById(order.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ORD_NOT_FOUND));

        // Update fields that might have changed
        o.resetForRetry(); // This also sets status and sagaStatus
        
        o.updateDetails(totalAmount, command.getShippingAddress(), command.getCustomerPhone());

        List<OrderItem> items = cart.getItems().stream()
                .map(item -> OrderItem.builder()
                        .order(o)
                        .bookId(item.getBookId())
                        .bookTitle(item.getTitle())
                        .priceAtPurchase(item.getPrice())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        o.setItems(items);
        return orderPersistencePort.save(o);
    }

    public Order saveInitialOrder(Long userId, CheckoutCommand command, CartPort.CartDto cart, BigDecimal totalAmount) {
        Order order = Order.builder()
                .userId(userId)
                .requestId(command.getRequestId())
                .fulfillmentStatus(FulfillmentStatus.PENDING)
                .sagaStatus(SagaStatus.INIT)
                .totalAmount(totalAmount)
                .discountAmount(BigDecimal.ZERO)
                .shippingAddress(command.getShippingAddress())
                .customerPhone(command.getCustomerPhone())
                .expiredAt(LocalDateTime.now().plusMinutes(15))
                .build();
        
        List<OrderItem> items = cart.getItems().stream()
                .map(item -> OrderItem.builder()
                        .order(order)
                        .bookId(item.getBookId())
                        .bookTitle(item.getTitle())
                        .priceAtPurchase(item.getPrice())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());
        
        order.setItems(items);
        return orderPersistencePort.save(order);
    }

    @Transactional
    public void updateSagaStatus(Order order, SagaStatus status) {
        Order o = orderPersistencePort.findById(order.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ORD_NOT_FOUND));
        
        if (status == SagaStatus.STOCK_RESERVED) o.markStockReserved();
        else if (status == SagaStatus.COUPON_RESERVED) o.markCouponReserved();
        else if (status == SagaStatus.FAILED) o.markFailed();
        
        orderPersistencePort.save(o);
    }

    @Transactional
    public void updateOrderWithPromotion(Order order, BigDecimal discount, SagaStatus status) {
        Order o = orderPersistencePort.findById(order.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ORD_NOT_FOUND));
        
        o.setDiscountAmount(discount);
        if (status == SagaStatus.COUPON_RESERVED) o.markCouponReserved();
        
        orderPersistencePort.save(o);
    }

    @Transactional
    public void completeSaga(Order order) {
        Order o = orderPersistencePort.findById(order.getId()).get();
        o.markSagaCompleted();
        orderPersistencePort.save(o);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderPersistencePort.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORD_NOT_FOUND));
        
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long userId) {
        List<Order> orders = orderPersistencePort.findByUserId(userId);
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrderById(Long orderId, Long userId) {
        Order order = orderPersistencePort.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ORD_NOT_FOUND));
        
        return mapToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelMyPendingOrder(Long orderId, Long userId, String reason) {
        Order order = orderPersistencePort.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ORD_NOT_FOUND));

        if (order.getFulfillmentStatus() != FulfillmentStatus.PENDING) {
            throw new InvalidOrderTransitionException("Chỉ có thể hủy đơn hàng khi chưa được xác nhận");
        }

        order.forceCancel(reason == null || reason.isBlank() ? "Customer cancelled pending order" : reason);
        restoreInventoryIfReserved(order);
        promotionPort.releaseCoupon(order.getRequestId());

        Order saved = orderPersistencePort.save(order);
        log.info("Order {} cancelled by customer {} before confirmation. Reason: {}", orderId, userId, reason);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void markOrderAsPaid(Long orderId) {
        Order order = orderPersistencePort.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORD_NOT_FOUND));
        
        order.confirm(); // V28: markPaid() đã bị xoá, dùng confirm() — FulfillmentStatus → CONFIRMED
        orderPersistencePort.save(order);
        log.info("Order {} fulfillment status set to CONFIRMED (via payment)", orderId);
    }

    // processReturnCompleted removed in V27 cleanup — ReturnRequest.returnStatus is now the
    // sole source of truth for return lifecycle. Order.fulfillmentStatus stays DELIVERED.

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderPersistencePort.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminOrderResponse> searchAdminOrders(AdminOrderSearchCriteria criteria) {
        FulfillmentStatus status = criteria == null ? null : criteria.getStatus();
        Map<Long, OrderUserPort.UserDto> userCache = new HashMap<>();
        return orderPersistencePort.searchAdminOrders(status).stream()
                .map(order -> mapToAdminResponse(order, userCache))
                .filter(order -> criteria == null || matchesCustomerKeyword(order, criteria.getCustomerKeyword()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderResponse getAdminOrderById(Long orderId) {
        Order order = orderPersistencePort.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORD_NOT_FOUND));
        return mapToAdminResponse(order, new HashMap<>());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopSellingBookResponse> getTopSellingBooks(int limit) {
        int effectiveLimit = limit > 0 ? limit : 5;
        List<FulfillmentStatus> paidStatuses = List.of(
                FulfillmentStatus.CONFIRMED,
                FulfillmentStatus.PROCESSING,
                FulfillmentStatus.DELIVERING,
                FulfillmentStatus.DELIVERED
        );
        return orderPersistencePort.findTopSellingBooks(paidStatuses, effectiveLimit).stream()
                .map(book -> new TopSellingBookResponse(
                        book.bookId(),
                        book.title(),
                        book.quantitySold(),
                        book.revenue() != null ? book.revenue() : BigDecimal.ZERO
                ))
                .collect(Collectors.toList());
    }

    private AdminOrderResponse mapToAdminResponse(Order order, Map<Long, OrderUserPort.UserDto> userCache) {
        OrderUserPort.UserDto user = userCache.computeIfAbsent(order.getUserId(), orderUserPort::getUserDetails);
        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;

        return AdminOrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .customerName(user.getFullName())
                .customerEmail(user.getEmail())
                .customerPhone(user.getPhoneNumber() != null ? user.getPhoneNumber() : order.getCustomerPhone())
                .shippingAddress(order.getShippingAddress())
                .totalAmount(order.getTotalAmount())
                .discountAmount(discount)
                .finalAmount(order.getTotalAmount().subtract(discount))
                .fulfillmentStatus(order.getFulfillmentStatus().name())
                .sagaStatus(order.getSagaStatus().name())
                .requestId(order.getRequestId())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(order.getItems().stream()
                        .map(item -> OrderItemResponse.builder()
                                .bookId(item.getBookId())
                                .title(item.getBookTitle())
                                .quantity(item.getQuantity())
                                .priceAtPurchase(item.getPriceAtPurchase())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private boolean matchesCustomerKeyword(AdminOrderResponse order, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        return Map.of(
                        "userId", String.valueOf(order.getUserId()),
                        "name", order.getCustomerName() != null ? order.getCustomerName() : "",
                        "email", order.getCustomerEmail() != null ? order.getCustomerEmail() : "",
                        "phone", order.getCustomerPhone() != null ? order.getCustomerPhone() : ""
                )
                .values()
                .stream()
                .anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(normalizedKeyword));
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateFulfillmentStatusCommand command) {
        Order order = orderPersistencePort.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORD_NOT_FOUND));

        FulfillmentStatus fromStatus = order.getFulfillmentStatus();
        FulfillmentStatus toStatus = command.getNewStatus();

        if (!Order.isValidAdminTransition(fromStatus, toStatus)) {
            throw new InvalidOrderTransitionException(
                String.format("Không thể chuyển fulfillment status từ %s sang %s", fromStatus, toStatus));
        }

        switch (toStatus) {
            case CONFIRMED -> order.confirm();
            case PROCESSING -> order.startProcessing();
            case DELIVERING -> order.startDelivering();
            case DELIVERED  -> order.markDelivered();
            case CANCELLED  -> {
                order.cancelByTransition();
                restoreInventoryIfReserved(order);
                promotionPort.releaseCoupon(order.getRequestId());
            }
            default -> throw new InvalidOrderTransitionException("Trạng thái không hợp lệ cho Admin transition");
        }

        Order saved = orderPersistencePort.save(order);
        eventPublisher.publishEvent(OrderFulfillmentStatusChangedEvent.of(
                saved,
                fromStatus,
                toStatus,
                command.getReason()
        ));
        log.info("Order {} fulfillmentStatus: {} → {} by Admin/Staff. Reason: {}",
            orderId, fromStatus, toStatus, command.getReason());
        publishAudit("STAFF_UPDATE_ORDER_STATUS", orderId, fromStatus, toStatus, command.getReason());
        
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId, String reason) {
        Order order = orderPersistencePort.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORD_NOT_FOUND));

        FulfillmentStatus currentStatus = order.getFulfillmentStatus();
        if (currentStatus == FulfillmentStatus.DELIVERED || currentStatus == FulfillmentStatus.CANCELLED) {
            throw new InvalidOrderTransitionException(
                String.format("Không thể hủy đơn hàng ở trạng thái %s", currentStatus));
        }

        order.forceCancel(reason);

        // Side effect: Restore inventory nếu stock đã bị trừ
        restoreInventoryIfReserved(order);
        promotionPort.releaseCoupon(order.getRequestId());

        Order saved = orderPersistencePort.save(order);
        eventPublisher.publishEvent(OrderFulfillmentStatusChangedEvent.of(
                saved,
                currentStatus,
                FulfillmentStatus.CANCELLED,
                reason
        ));
        log.info("Order {} force-cancelled from {} by Admin/Staff. Reason: {}",
            orderId, currentStatus, reason);
        publishAudit("STAFF_CANCEL_ORDER", orderId, currentStatus, FulfillmentStatus.CANCELLED, reason);

        return mapToResponse(saved);
    }

    private void publishAudit(
            String action,
            Long orderId,
            FulfillmentStatus fromStatus,
            FulfillmentStatus toStatus,
            String reason
    ) {
        auditEventPublisherPort.publish(new UserActionAuditedEvent(
                getCurrentUserIdForAudit(),
                getCurrentRoleForAudit(),
                action,
                String.valueOf(orderId),
                "Trạng thái đơn hàng: " + fromStatus,
                "Trạng thái đơn hàng: " + fromStatus + " -> " + toStatus + formatReason(reason),
                Instant.now()
        ));
    }

    private String formatReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "";
        }
        return ". Lý do: " + reason;
    }

    private String getCurrentUserIdForAudit() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private String getCurrentRoleForAudit() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "";
        }
        if (authentication.getPrincipal() instanceof UserPrincipal principal && principal.role() != null) {
            return principal.role();
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse("");
    }

    private void restoreInventoryIfReserved(Order order) {
        if (order.getSagaStatus() == SagaStatus.INIT || order.getSagaStatus() == SagaStatus.FAILED) {
            return;
        }

        List<InventoryPort.StockItem> stockItems = order.getItems().stream()
                .map(item -> InventoryPort.StockItem.builder()
                        .bookId(item.getBookId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();
        inventoryPort.increaseStockBulk(stockItems, order.getRequestId());
        log.info("Inventory restored for cancelled order {}", order.getId());
    }
}
