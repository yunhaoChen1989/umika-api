package ca.umika.api.menu;

import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/v1/manager/menu-options")
public class MenuOptionController {
    private final MenuOptionService service;

    public MenuOptionController(MenuOptionService service) { this.service = service; }

    @GetMapping
    public Page<MenuOptionDto> findAll(Authentication authentication, @RequestParam UUID categoryId,
            Pageable pageable) {
        return service.findAll(authentication, categoryId, pageable);
    }

    @PostMapping
    public ResponseEntity<MenuOptionDto> create(Authentication authentication, @RequestBody MenuOptionDto dto) {
        MenuOptionDto created = service.create(authentication, dto);
        return ResponseEntity.created(URI.create("/api/v1/manager/menu-options/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public MenuOptionDto update(Authentication authentication, @PathVariable UUID id,
            @RequestBody MenuOptionDto dto) {
        return service.update(authentication, id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(authentication, id);
        return ResponseEntity.noContent().build();
    }
}

