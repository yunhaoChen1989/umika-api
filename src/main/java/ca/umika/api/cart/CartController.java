package ca.umika.api.cart;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    public ResponseEntity<CartResponse> getOrCreateActiveCart(
            Authentication authentication,
            @RequestBody CartCreateRequest request
    ) {
        CartResponse cart = cartService.getOrCreateActiveCart(authentication, request);
        return ResponseEntity.created(URI.create("/api/v1/cart/" + cart.id())).body(cart);
    }

    @GetMapping("/{id}")
    public CartResponse getCart(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestParam(required = false) String sessionId
    ) {
        return cartService.getCart(authentication, id, sessionId);
    }

    @PostMapping("/{cartId}/items")
    public CartResponse addItem(
            Authentication authentication,
            @PathVariable UUID cartId,
            @RequestBody CartAddItemRequest request,
            @RequestParam(required = false) String sessionId
    ) {
        return cartService.addItem(authentication, cartId, request, sessionId);
    }

    @PutMapping("/{cartId}/items/{itemId}")
    public CartResponse updateItem(
            Authentication authentication,
            @PathVariable UUID cartId,
            @PathVariable UUID itemId,
            @RequestBody CartUpdateItemRequest request,
            @RequestParam(required = false) String sessionId
    ) {
        return cartService.updateItem(authentication, cartId, itemId, request, sessionId);
    }

    @DeleteMapping("/{cartId}/items/{itemId}")
    public CartResponse deleteItem(
            Authentication authentication,
            @PathVariable UUID cartId,
            @PathVariable UUID itemId,
            @RequestParam(required = false) String sessionId
    ) {
        return cartService.deleteItem(authentication, cartId, itemId, sessionId);
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<Void> abandonCart(
            Authentication authentication,
            @PathVariable UUID cartId,
            @RequestParam(required = false) String sessionId
    ) {
        cartService.abandonCart(authentication, cartId, sessionId);
        return ResponseEntity.noContent().build();
    }
}
