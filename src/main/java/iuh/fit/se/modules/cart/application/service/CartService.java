package iuh.fit.se.modules.cart.application.service;

import iuh.fit.se.modules.cart.application.port.in.CartInternalUseCase;
import iuh.fit.se.modules.cart.application.port.out.CartPersistencePort;
import iuh.fit.se.modules.cart.domain.Cart;
import iuh.fit.se.modules.catalog.application.port.in.BookDTO;
import iuh.fit.se.modules.catalog.application.port.in.BookUseCase;
import iuh.fit.se.modules.inventory.application.port.in.InventoryInternalUseCase;
import iuh.fit.se.modules.inventory.application.port.in.StockResult;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService implements CartInternalUseCase {

    private final CartPersistencePort cartPersistencePort;
    private final BookUseCase bookUseCase; // Catalog Integration
    private final InventoryInternalUseCase inventoryInternalUseCase; // Inventory Integration
    
    private static final int MAX_PER_ITEM = 10; // Cấu hình tối đa 10 cuốn mỗi loại trong giỏ hàng

    @Override
    @Transactional
    public CartResponse getCartByUserId(Long userId) {
        Cart cart = cartPersistencePort.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Creating new cart for user {}", userId);
                    Cart newCart = Cart.create(userId);
                    cartPersistencePort.save(newCart);
                    return newCart;
                });
        
        return mapToResponse(cart);
    }

    private CartResponse mapToResponse(Cart cart) {
        return CartResponse.builder()
                .userId(cart.getUserId())
                .totalAmount(cart.calculateTotal())
                .items(cart.getItems().stream()
                        .map(item -> CartItemResponse.builder()
                                .bookId(item.getBookId())
                                .title(item.getTitleSnapshot())
                                .price(item.getPriceAtAddTime())
                                .quantity(item.getQuantity())
                                .build())
                        .collect(java.util.stream.Collectors.toList()))
                .build();
    }

    private Cart getCartEntity(Long userId) {
        return cartPersistencePort.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.create(userId);
                    cartPersistencePort.save(newCart);
                    return newCart;
                });
    }

    @Override
    @Transactional
    public void addItem(Long userId, AddItemCommand command) {
        // 1. Lấy thực thể Cart
        Cart cart = getCartEntity(userId);

        // 2. Validate Book tồn tại và Active (Catalog Integration)
        BookDTO book = bookUseCase.getBook(command.getBookId());
        if (book == null) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 3. Soft-check Inventory (Inventory Integration - Redis Cache)
        StockResult stock = inventoryInternalUseCase.getAvailableStock(command.getBookId());
        if (stock.getStatus() != StockResult.Status.SUCCESS || stock.getRemainingQuantity() < command.getQuantity()) {
            throw new AppException(ErrorCode.INV_OUT_OF_STOCK);
        }

        // 4. Áp dụng Domain Logic
        cart.addItem(book.id(), command.getQuantity(), book.price(), book.title(), MAX_PER_ITEM);
        
        // 5. Save
        cartPersistencePort.save(cart);
    }

    @Override
    @Transactional
    public void updateItemQuantity(Long userId, UpdateQuantityCommand command) {
        Cart cart = getCartEntity(userId);
        
        // Soft-check Inventory nếu tăng số lượng
        StockResult stock = inventoryInternalUseCase.getAvailableStock(command.getBookId());
        if (stock.getStatus() == StockResult.Status.SUCCESS && stock.getRemainingQuantity() < command.getQuantity()) {
            throw new AppException(ErrorCode.INV_OUT_OF_STOCK);
        }

        cart.updateItemQuantity(command.getBookId(), command.getQuantity(), MAX_PER_ITEM);
        cartPersistencePort.save(cart);
    }

    @Override
    @Transactional
    public void removeItem(Long userId, Long bookId) {
        cartPersistencePort.findByUserId(userId)
                .ifPresent(cart -> {
                    cart.removeItem(bookId);
                    cartPersistencePort.save(cart);
                });
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        log.info("Clearing cart for user: {}", userId);
        cartPersistencePort.findByUserId(userId)
                .ifPresent(cart -> {
                    cart.clearItems();
                    cartPersistencePort.save(cart);
                    log.info("Cart cleared successfully for user: {}", userId);
                });
    }
}
