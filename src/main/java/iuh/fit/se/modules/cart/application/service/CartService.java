package iuh.fit.se.modules.cart.application.service;

import iuh.fit.se.modules.cart.application.port.in.CartInternalUseCase;
import iuh.fit.se.modules.cart.application.port.out.CartPersistencePort;
import iuh.fit.se.modules.cart.domain.Cart;
import iuh.fit.se.modules.catalog.application.port.in.BookDTO;
import iuh.fit.se.modules.catalog.application.port.in.BookUseCase;
import iuh.fit.se.modules.inventory.application.port.in.InventoryInternalUseCase;
import iuh.fit.se.modules.inventory.application.port.in.StockResult;
import iuh.fit.se.modules.promotion.application.port.in.FlashSaleUseCase;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService implements CartInternalUseCase {

    private final CartPersistencePort cartPersistencePort;
    private final BookUseCase bookUseCase; // Catalog Integration
    private final InventoryInternalUseCase inventoryInternalUseCase; // Inventory Integration
    private final FlashSaleUseCase flashSaleUseCase; // Promotion Integration

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
        java.util.List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> {
                    BookDTO book = bookUseCase.getBook(item.getBookId());
                    BigDecimal currentPrice = book != null
                            ? flashSaleUseCase.reserveActiveSalePriceOrRegular(item.getBookId(), item.getQuantity(), book.price())
                            : item.getPriceAtAddTime();
                    return CartItemResponse.builder()
                            .bookId(item.getBookId())
                            .title(item.getTitleSnapshot())
                            .price(currentPrice)
                            .quantity(item.getQuantity())
                            .build();
                })
                .collect(java.util.stream.Collectors.toList());

        return CartResponse.builder()
                .userId(cart.getUserId())
                .totalAmount(items.stream()
                        .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .items(items)
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

        int targetQuantity = validateCartQuantityLimit(cart, book, command.getQuantity());

        // 4. Áp dụng Domain Logic - dùng tồn kho sách thực tế làm giới hạn
        BigDecimal effectivePrice = flashSaleUseCase.reserveActiveSalePriceOrRegular(
                book.id(),
                targetQuantity,
                book.price()
        );
        addItemToCart(cart, book, command.getQuantity(), effectivePrice);
        
        // 5. Save
        cartPersistencePort.save(cart);
    }

    private int validateCartQuantityLimit(Cart cart, BookDTO book, int addingQuantity) {
        int currentQuantity = cart.getItems().stream()
                .filter(item -> item.getBookId().equals(book.id()))
                .mapToInt(item -> item.getQuantity())
                .findFirst()
                .orElse(0);
        int targetQuantity = currentQuantity + addingQuantity;
        if (targetQuantity > book.quantity()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "So luong sach vuot qua gioi han toi da");
        }
        return targetQuantity;
    }

    @Override
    @Transactional
    public void addFlashSaleItem(Long userId, AddItemCommand command) {
        Cart cart = getCartEntity(userId);

        BookDTO book = bookUseCase.getBook(command.getBookId());
        if (book == null) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        StockResult stock = inventoryInternalUseCase.getAvailableStock(command.getBookId());
        if (stock.getStatus() != StockResult.Status.SUCCESS || stock.getRemainingQuantity() < command.getQuantity()) {
            throw new AppException(ErrorCode.INV_OUT_OF_STOCK);
        }

        int targetQuantity = validateCartQuantityLimit(cart, book, command.getQuantity());
        BigDecimal salePrice = flashSaleUseCase.reserveActiveSalePrice(book.id(), targetQuantity, book.price());
        addItemToCart(cart, book, command.getQuantity(), salePrice);
        cartPersistencePort.save(cart);
    }

    private void addItemToCart(Cart cart, BookDTO book, int quantity, BigDecimal price) {
        try {
            cart.addItem(book.id(), quantity, price, book.title(), book.quantity());
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_INPUT, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateItemQuantity(Long userId, UpdateQuantityCommand command) {
        Cart cart = getCartEntity(userId);
        
        // Lấy tồn kho sách thực tế để làm giới hạn
        BookDTO book = bookUseCase.getBook(command.getBookId());
        int maxQuantity = (book != null) ? book.quantity() : command.getQuantity();

        // Soft-check Inventory nếu tăng số lượng
        StockResult stock = inventoryInternalUseCase.getAvailableStock(command.getBookId());
        if (stock.getStatus() == StockResult.Status.SUCCESS && stock.getRemainingQuantity() < command.getQuantity()) {
            throw new AppException(ErrorCode.INV_OUT_OF_STOCK);
        }

        cart.updateItemQuantity(command.getBookId(), command.getQuantity(), maxQuantity);
        if (book != null && command.getQuantity() > 0) {
            BigDecimal effectivePrice = flashSaleUseCase.reserveActiveSalePriceOrRegular(
                    book.id(),
                    command.getQuantity(),
                    book.price()
            );
            cart.getItems().stream()
                    .filter(item -> item.getBookId().equals(book.id()))
                    .findFirst()
                    .ifPresent(item -> item.replacePrice(effectivePrice, book.title()));
        }
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
