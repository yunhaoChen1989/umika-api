package ca.umika.api.order;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/orders", "/api/orders"})
@Tag(name = "Order")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public Page<OrderResponse> findAll(
            Authentication authentication,
            Pageable pageable,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) String status
    ) {
        return service.findAll(authentication, pageable, userEmail, email, locationId, status);
    }

    @GetMapping("/{id}")
    public OrderResponse findById(Authentication authentication, @PathVariable UUID id) {
        return service.findById(authentication, id);
    }

    @PostMapping("/redemption-preview")
    public OrderRedemptionPreviewResponse previewRedemption(
            Authentication authentication,
            @RequestBody OrderRedemptionPreviewRequest request
    ) {
        return service.previewRedemption(authentication, request);
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            Authentication authentication,
            @RequestBody OrderCheckoutRequest request
    ) {
        OrderResponse created = service.checkout(authentication, request);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + created.id())).body(created);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody OrderStatusUpdateRequest request
    ) {
        return service.updateStatus(authentication, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(authentication, id);
        return ResponseEntity.noContent().build();
    }
}
