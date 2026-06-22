package ca.umika.api.store;

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
@RequestMapping("/api/v1/business-hours")
@Tag(name = "BusinessHour")
public class BusinessHourController {

    private final BusinessHourService service;

    public BusinessHourController(BusinessHourService service) {
        this.service = service;
    }

    @GetMapping
    public List<BusinessHourDto> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public BusinessHourDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<BusinessHourDto> create(@RequestBody BusinessHourDto dto) {
        BusinessHourDto created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/business-hours/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public BusinessHourDto update(@PathVariable UUID id, @RequestBody BusinessHourDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
