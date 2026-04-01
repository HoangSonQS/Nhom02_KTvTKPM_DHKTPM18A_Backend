package iuh.fit.se.modules.cart.application.port.out;

import iuh.fit.se.modules.cart.domain.Cart;
import java.util.Optional;

public interface CartPersistencePort {
    Optional<Cart> findByUserId(Long userId);
    void save(Cart cart);
    void deleteByUserId(Long userId);
}
