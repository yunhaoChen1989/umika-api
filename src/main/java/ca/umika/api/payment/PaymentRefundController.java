package ca.umika.api.payment;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/orders", "/api/v1/manager/orders", "/api/orders"})
@Tag(name = "OrderRefund")
public class PaymentRefundController {

    private final PaymentRefundService service;

    public PaymentRefundController(PaymentRefundService service) {
        this.service = service;
    }

    @GetMapping("/{orderId}/refunds")
    public List<PaymentRefundDto> findByOrder(
            Authentication authentication,
            @PathVariable UUID orderId
    ) {
        return service.findByOrder(authentication, orderId);
    }

    @PostMapping("/{orderId}/refunds")
    public ResponseEntity<PaymentRefundDto> refund(
            Authentication authentication,
            @PathVariable UUID orderId,
            @RequestBody PaymentRefundRequest request
    ) {
        PaymentRefundDto created = service.refund(authentication, orderId, request);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + orderId + "/refunds/" + created.id())).body(created);
    }
}
