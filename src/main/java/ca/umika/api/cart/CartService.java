package ca.umika.api.cart;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @Transactional
    public CartEntity createCart(CartEntity cart) {
        return cartRepository.save(cart);
    }

    public Optional<CartEntity> getCart(UUID id) {
        return cartRepository.findById(id);
    }

    public List<CartEntity> getAllCarts() {
        return cartRepository.findAll();
    }

    @Transactional
    public CartEntity updateCart(CartEntity cart) {
        return cartRepository.save(cart);
    }

    @Transactional
    public void deleteCart(UUID id) {
        cartRepository.deleteById(id);
    }

    // Cart items management
    @Transactional
    public CartItemEntity addItemToCart(CartItemEntity item) {
        return cartItemRepository.save(item);
    }

    public Page<CartItemEntity> getItemsForCart(UUID cartId, Pageable pageable) {
        return cartItemRepository.findByCart_Id(cartId, pageable);
    }

    @Transactional
    public void deleteItem(UUID itemId) {
        cartItemRepository.deleteById(itemId);
    }
}
