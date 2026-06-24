package ca.umika.api.cart;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // Create a new cart
    @PostMapping
    public ResponseEntity<CartEntity> createCart(@RequestBody CartEntity cart) {
        CartEntity created = cartService.createCart(cart);
        return ResponseEntity.ok(created);
    }

    // Get cart by ID
    @GetMapping("/{id}")
    public ResponseEntity<CartEntity> getCart(@PathVariable UUID id) {
        Optional<CartEntity> cart = cartService.getCart(id);
        return cart.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update cart
    @PutMapping("/{id}")
    public ResponseEntity<CartEntity> updateCart(@PathVariable UUID id, @RequestBody CartEntity cart) {
        cart.setId(id);
        CartEntity updated = cartService.updateCart(cart);
        return ResponseEntity.ok(updated);
    }

    // Delete cart
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCart(@PathVariable UUID id) {
        cartService.deleteCart(id);
        return ResponseEntity.noContent().build();
    }

    // Add item to cart
    @PostMapping("/{cartId}/items")
    public ResponseEntity<CartItemEntity> addItem(@PathVariable UUID cartId, @RequestBody CartItemEntity item) {
        // associate cart
        CartEntity cart = cartService.getCart(cartId).orElseThrow(() -> new RuntimeException("Cart not found"));
        item.setCart(cart);
        CartItemEntity saved = cartService.addItemToCart(item);
        return ResponseEntity.ok(saved);
    }

    // Get items for a cart
    @GetMapping("/{cartId}/items")
    public ResponseEntity<Page<CartItemEntity>> getItems(@PathVariable UUID cartId, Pageable pageable) {
        Page<CartItemEntity> items = cartService.getItemsForCart(cartId, pageable);
        return ResponseEntity.ok(items);
    }

    // Delete an item from cart
    @DeleteMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID cartId, @PathVariable UUID itemId) {
        // optional check that item belongs to cart could be added
        cartService.deleteItem(itemId);
        return ResponseEntity.noContent().build();
    }
}
