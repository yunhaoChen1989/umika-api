package ca.umika.api.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
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
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/v1/admin/system-menus")
@Tag(name = "SystemMenu")
public class SystemMenuController {

    private final SystemMenuService service;

    public SystemMenuController(SystemMenuService service) {
        this.service = service;
    }

    @GetMapping
    public Page<SystemMenuDto> findAll(Authentication authentication, Pageable pageable) {
        return service.findAll(authentication, pageable);
    }

    @GetMapping("/{id}")
    public SystemMenuDto findById(Authentication authentication, @PathVariable UUID id) {
        return service.findById(authentication, id);
    }

    @PostMapping
    public ResponseEntity<SystemMenuDto> create(Authentication authentication, @RequestBody SystemMenuUpsertRequest request) {
        SystemMenuDto created = service.create(authentication, request);
        return ResponseEntity.created(URI.create("/api/v1/admin/system-menus/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public SystemMenuDto update(Authentication authentication, @PathVariable UUID id, @RequestBody SystemMenuUpsertRequest request) {
        return service.update(authentication, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(authentication, id);
        return ResponseEntity.noContent().build();
    }
}
