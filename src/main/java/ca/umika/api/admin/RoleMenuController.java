package ca.umika.api.admin;

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
@RequestMapping("/api/v1/role-menus")
@Tag(name = "RoleMenu")
public class RoleMenuController {

    private final RoleMenuService service;

    public RoleMenuController(RoleMenuService service) {
        this.service = service;
    }

    @GetMapping
    public Page<RoleMenuDto> findAll(Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{roleId}/{menuId}")
    public RoleMenuDto findById(@PathVariable UUID roleId, @PathVariable UUID menuId) {
        return service.findById(roleId, menuId);
    }

    @PostMapping
    public ResponseEntity<RoleMenuDto> create(@RequestBody RoleMenuDto dto) {
        RoleMenuDto created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/role-menus/" + created.roleId() + "/" + created.menuId())).body(created);
    }

    @DeleteMapping("/{roleId}/{menuId}")
    public ResponseEntity<Void> delete(@PathVariable UUID roleId, @PathVariable UUID menuId) {
        service.delete(roleId, menuId);
        return ResponseEntity.noContent().build();
    }
}
