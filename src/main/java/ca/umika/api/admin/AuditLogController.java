package ca.umika.api.admin;

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
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "AuditLog")
public class AuditLogController {

    private final AuditLogService service;

    public AuditLogController(AuditLogService service) {
        this.service = service;
    }

    @GetMapping
    public List<AuditLogDto> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AuditLogDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<AuditLogDto> create(@RequestBody AuditLogDto dto) {
        AuditLogDto created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/audit-logs/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public AuditLogDto update(@PathVariable UUID id, @RequestBody AuditLogDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
