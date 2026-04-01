package iuh.fit.se.modules.cart.adapter.outbound.persistence;

import iuh.fit.se.modules.cart.application.port.out.CartPersistencePort;
import iuh.fit.se.modules.cart.domain.Cart;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CartPersistenceAdapter implements CartPersistencePort {

    private final CartJpaRepository cartJpaRepository;

    @Override
    public Optional<Cart> findByUserId(Long userId) {
        return cartJpaRepository.findByUserId(userId);
    }

    @Override
    public void save(Cart cart) {
        cartJpaRepository.save(cart);
    }

    @Override
    public void deleteByUserId(Long userId) {
        cartJpaRepository.findByUserId(userId)
                .ifPresent(cartJpaRepository::delete);
    }
}
