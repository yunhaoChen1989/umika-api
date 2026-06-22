package ca.umika.api.menu;

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
@RequestMapping("/api/v1/menu-item-images")
@Tag(name = "MenuItemImage")
public class MenuItemImageController {

    private final MenuItemImageService service;

    public MenuItemImageController(MenuItemImageService service) {
        this.service = service;
    }

    @GetMapping
    public List<MenuItemImageDto> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public MenuItemImageDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<MenuItemImageDto> create(@RequestBody MenuItemImageDto dto) {
        MenuItemImageDto created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/menu-item-images/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public MenuItemImageDto update(@PathVariable UUID id, @RequestBody MenuItemImageDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
