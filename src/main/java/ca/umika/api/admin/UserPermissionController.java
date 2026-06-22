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
@RequestMapping("/api/v1/user-permissions")
@Tag(name = "UserPermission")
public class UserPermissionController {

    private final UserPermissionService service;

    public UserPermissionController(UserPermissionService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserPermissionDto> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public UserPermissionDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<UserPermissionDto> create(@RequestBody UserPermissionDto dto) {
        UserPermissionDto created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/user-permissions/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public UserPermissionDto update(@PathVariable UUID id, @RequestBody UserPermissionDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
