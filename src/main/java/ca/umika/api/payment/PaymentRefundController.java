package ca.umika.api.payment;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/v1/payment-refunds")
@Tag(name = "PaymentRefund")
public class PaymentRefundController {

    private final PaymentRefundService service;

    public PaymentRefundController(PaymentRefundService service) {
        this.service = service;
    }

    @GetMapping
    public Page<PaymentRefundDto> findAll(Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    public PaymentRefundDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<PaymentRefundDto> create(@RequestBody PaymentRefundDto dto) {
        PaymentRefundDto created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/payment-refunds/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public PaymentRefundDto update(@PathVariable UUID id, @RequestBody PaymentRefundDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
