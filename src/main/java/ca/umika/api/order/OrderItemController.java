package ca.umika.api.order;

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
@RequestMapping("/api/v1/order-items")
@Tag(name = "OrderItem")
public class OrderItemController {

    private final OrderItemService service;

    public OrderItemController(OrderItemService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrderItemDto> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public OrderItemDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<OrderItemDto> create(@RequestBody OrderItemDto dto) {
        OrderItemDto created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/order-items/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public OrderItemDto update(@PathVariable UUID id, @RequestBody OrderItemDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
