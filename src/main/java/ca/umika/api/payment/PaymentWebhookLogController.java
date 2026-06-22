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
@RequestMapping("/api/v1/payment-webhook-logs")
@Tag(name = "PaymentWebhookLog")
public class PaymentWebhookLogController {

    private final PaymentWebhookLogService service;

    public PaymentWebhookLogController(PaymentWebhookLogService service) {
        this.service = service;
    }

    @GetMapping
    public List<PaymentWebhookLogDto> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PaymentWebhookLogDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<PaymentWebhookLogDto> create(@RequestBody PaymentWebhookLogDto dto) {
        PaymentWebhookLogDto created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/payment-webhook-logs/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public PaymentWebhookLogDto update(@PathVariable UUID id, @RequestBody PaymentWebhookLogDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
