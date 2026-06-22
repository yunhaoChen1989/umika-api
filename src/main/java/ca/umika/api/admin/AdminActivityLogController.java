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
@RequestMapping("/api/v1/admin-activity-logs")
@Tag(name = "AdminActivityLog")
public class AdminActivityLogController {

    private final AdminActivityLogService service;

    public AdminActivityLogController(AdminActivityLogService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminActivityLogDto> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AdminActivityLogDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<AdminActivityLogDto> create(@RequestBody AdminActivityLogDto dto) {
        AdminActivityLogDto created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/admin-activity-logs/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public AdminActivityLogDto update(@PathVariable UUID id, @RequestBody AdminActivityLogDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
