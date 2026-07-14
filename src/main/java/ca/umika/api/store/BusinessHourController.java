package ca.umika.api.store;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@RequestMapping({"/api/v1/business-hours", "/api/v1/manager/business-hours"})
@Tag(name = "BusinessHour")
public class BusinessHourController {

    private final BusinessHourService service;

    public BusinessHourController(BusinessHourService service) {
        this.service = service;
    }

    @GetMapping
    public Page<BusinessHourDto> findAll(Pageable pageable, @RequestParam(required = false) UUID locationId) {
        return service.findAll(pageable, locationId);
    }

    @GetMapping("/{id}")
    public BusinessHourDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<BusinessHourDto> create(Authentication authentication, @RequestBody BusinessHourDto dto) {
        BusinessHourDto created = service.create(authentication, dto);
        return ResponseEntity.created(URI.create("/api/v1/business-hours/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public BusinessHourDto update(Authentication authentication, @PathVariable UUID id, @RequestBody BusinessHourDto dto) {
        return service.update(authentication, id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(authentication, id);
        return ResponseEntity.noContent().build();
    }
}
