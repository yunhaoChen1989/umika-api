package ca.umika.api.auth;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/user-roles", "/api/v1/manager/user-roles"})
@Tag(name = "UserRole")
public class UserRoleController {

    private final UserRoleService service;

    public UserRoleController(UserRoleService service) {
        this.service = service;
    }

    @GetMapping
    public Page<UserRoleDto> findAll(Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{userId}/{roleId}")
    public UserRoleDto findById(@PathVariable UUID userId, @PathVariable UUID roleId) {
        return service.findById(userId, roleId);
    }

    @PostMapping
    public ResponseEntity<UserRoleDto> create(@RequestBody UserRoleDto dto) {
        UserRoleDto created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/user-roles/" + created.userId() + "/" + created.roleId())).body(created);
    }

    @DeleteMapping("/{userId}/{roleId}")
    public ResponseEntity<Void> delete(@PathVariable UUID userId, @PathVariable UUID roleId) {
        service.delete(userId, roleId);
        return ResponseEntity.noContent().build();
    }
}
