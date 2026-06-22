package ca.umika.api.payment;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payment-idempotency-keys")
@Tag(name = "PaymentIdempotencyKey")
public class PaymentIdempotencyKeyController {

    private final PaymentIdempotencyKeyService service;

    public PaymentIdempotencyKeyController(PaymentIdempotencyKeyService service) {
        this.service = service;
    }

    @GetMapping
    public List<PaymentIdempotencyKeyDto> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PaymentIdempotencyKeyDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<PaymentIdempotencyKeyDto> create(@RequestBody PaymentIdempotencyKeyDto dto) {
        PaymentIdempotencyKeyDto created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/payment-idempotency-keys/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public PaymentIdempotencyKeyDto update(@PathVariable UUID id, @RequestBody PaymentIdempotencyKeyDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
