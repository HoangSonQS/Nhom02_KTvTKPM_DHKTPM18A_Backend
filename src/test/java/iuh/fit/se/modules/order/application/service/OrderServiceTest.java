package iuh.fit.se.modules.order.application.service;

import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase;
import iuh.fit.se.modules.order.application.port.out.*;
import iuh.fit.se.modules.order.domain.Order;
import iuh.fit.se.modules.order.domain.FulfillmentStatus;
import iuh.fit.se.modules.order.domain.SagaStatus;
import iuh.fit.se.shared.audit.application.port.out.AuditEventPublisherPort;
import iuh.fit.se.shared.audit.domain.event.UserActionAuditedEvent;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderPersistencePort orderPersistencePort;
    @Mock private InventoryPort inventoryPort;
    @Mock private PromotionPort promotionPort;
    @Mock private CartPort cartPort;
    @Mock private OrderUserPort orderUserPort;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AuditEventPublisherPort auditEventPublisherPort;

    @InjectMocks private OrderService orderService;

    private Long userId = 1L;
    private OrderInternalUseCase.CheckoutCommand command;
    private CartPort.CartDto cartDto;
    private Order sharedOrder;

    @BeforeEach
    void setUp() {
        command = OrderInternalUseCase.CheckoutCommand.builder()
                .shippingAddress("123 Test St")
                .customerPhone("0901234567")
                .requestId("req-123")
                .build();

        cartDto = CartPort.CartDto.builder()
                .userId(userId)
                .items(List.of(CartPort.CartItemDto.builder()
                        .bookId(101L)
                        .title("Clean Code")
                        .price(new BigDecimal("100000"))
                        .quantity(2)
                        .build()))
                .build();

        // Quan trọng: Phải gán ID cho Order để mock findById làm việc đúng
        sharedOrder = null; 

        // Mock mặc định: Khi save thì gán ID giả lập và lưu vào sharedOrder
        lenient().when(orderPersistencePort.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (sharedOrder == null) {
                // Giả lập database sinh ID bằng reflection
                try {
                    java.lang.reflect.Field idField = Order.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(o, 1L);
                    sharedOrder = o;
                } catch (Exception e) {
                    // Ignore reflection errors in test
                }
            }
            return o; // return the order being saved
        });

        // Mock findById trả về sharedOrder (đối tượng duy nhất đang xử lý)
        lenient().when(orderPersistencePort.findById(anyLong())).thenAnswer(invocation -> Optional.ofNullable(sharedOrder));

        // Mock OrderUserPort
        lenient().when(orderUserPort.getUserDetails(anyLong())).thenReturn(OrderUserPort.UserDto.builder()
                .fullName("Test User")
                .email("test@example.com")
                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCheckout_Success() {
        // Given
        when(cartPort.getCartByUserId(userId)).thenReturn(cartDto);
        when(orderPersistencePort.findByRequestId(anyString())).thenReturn(Optional.empty());
        doNothing().when(inventoryPort).decreaseStockBulk(anyList(), anyString());

        // When
        OrderInternalUseCase.OrderResponse response = orderService.checkout(userId, command);

        // Then
        assertNotNull(response);
        assertNotNull(response.getOrderId());
        assertEquals(SagaStatus.COMPLETED, sharedOrder.getSagaStatus());
        assertEquals(FulfillmentStatus.PENDING, sharedOrder.getFulfillmentStatus());
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
    }

    @Test
    void testCheckout_InventoryFail_ShouldStopSaga() {
        // Given
        when(cartPort.getCartByUserId(userId)).thenReturn(cartDto);
        when(orderPersistencePort.findByRequestId(anyString())).thenReturn(Optional.empty());
        doThrow(new RuntimeException("Stock fail")).when(inventoryPort).decreaseStockBulk(anyList(), anyString());

        // When & Then
        assertThrows(Exception.class, () -> orderService.checkout(userId, command));

        assertNotNull(sharedOrder);
        assertEquals(SagaStatus.FAILED, sharedOrder.getSagaStatus());
        verify(promotionPort, never()).reserveCoupon(any(), any(), anyString());
    }

    @Test
    void testCheckout_PromotionFail_ShouldRollbackStock() {
        // Given
        command.setCouponCode("INVALID");
        when(cartPort.getCartByUserId(userId)).thenReturn(cartDto);
        when(orderPersistencePort.findByRequestId(anyString())).thenReturn(Optional.empty());
        doNothing().when(inventoryPort).decreaseStockBulk(anyList(), anyString());
        when(promotionPort.reserveCoupon(anyString(), any(), anyString()))
                .thenReturn(PromotionPort.PromotionResult.builder().success(false).message("Coupon expired").build());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> orderService.checkout(userId, command));
        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());

        // Verify stock rollback and Saga FAILED
        verify(inventoryPort).increaseStockBulk(anyList(), eq(command.getRequestId()));
        assertNotNull(sharedOrder);
        assertEquals(SagaStatus.FAILED, sharedOrder.getSagaStatus());
    }

    @Test
    void givenOrderStatusChanged_whenUpdateOrderStatus_thenAuditValueDescribesDiff() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "seller@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_STAFF_SELLER"))));
        sharedOrder = Order.builder()
                .id(10L)
                .userId(userId)
                .requestId("req-order-10")
                .fulfillmentStatus(FulfillmentStatus.CONFIRMED)
                .sagaStatus(SagaStatus.COMPLETED)
                .totalAmount(new BigDecimal("100000"))
                .discountAmount(BigDecimal.ZERO)
                .shippingAddress("123 Test St")
                .customerPhone("0901234567")
                .expiredAt(java.time.LocalDateTime.now().plusHours(1))
                .items(List.of())
                .build();

        OrderInternalUseCase.UpdateFulfillmentStatusCommand statusCommand =
                OrderInternalUseCase.UpdateFulfillmentStatusCommand.builder()
                        .newStatus(FulfillmentStatus.PROCESSING)
                        .reason("Đơn hàng đã được xác nhận và đang xử lý")
                        .build();

        orderService.updateOrderStatus(10L, statusCommand);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(auditEventPublisherPort).publish(eventCaptor.capture());
        UserActionAuditedEvent event = (UserActionAuditedEvent) eventCaptor.getValue();

        assertEquals("STAFF_UPDATE_ORDER_STATUS", event.action());
        assertEquals("10", event.target());
        assertEquals("Trạng thái đơn hàng: CONFIRMED", event.oldValue());
        assertEquals(
                "Trạng thái đơn hàng: CONFIRMED -> PROCESSING. Lý do: Đơn hàng đã được xác nhận và đang xử lý",
                event.newValue());
    }

    @Test
    void givenDateRange_whenGetBookSales_thenQueriesPaidStatusesAndInclusiveDates() {
        when(orderPersistencePort.findBookSales(
                anyList(),
                eq(LocalDateTime.of(2026, 5, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 6, 1, 0, 0))))
                .thenReturn(List.of(new OrderPersistencePort.TopSellingBookProjection(
                        101L,
                        "Clean Code",
                        7L,
                        new BigDecimal("700000"))));

        List<OrderInternalUseCase.BookSalesResponse> result = orderService.getBookSales(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31));

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).bookId());
        assertEquals(7L, result.get(0).quantitySold());
        assertEquals(0, new BigDecimal("700000").compareTo(result.get(0).revenue()));
        verify(orderPersistencePort).findBookSales(
                argThat(statuses -> statuses.size() == 1
                        && statuses.contains(FulfillmentStatus.DELIVERED)
                        && !statuses.contains(FulfillmentStatus.CONFIRMED)
                        && !statuses.contains(FulfillmentStatus.PROCESSING)
                        && !statuses.contains(FulfillmentStatus.DELIVERING)
                        && !statuses.contains(FulfillmentStatus.PENDING)
                        && !statuses.contains(FulfillmentStatus.CANCELLED)),
                eq(LocalDateTime.of(2026, 5, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 6, 1, 0, 0)));
    }
}
