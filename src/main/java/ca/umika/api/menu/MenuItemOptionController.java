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
@RequestMapping("/api/v1/menu-item-options")
@Tag(name = "MenuItemOption")
public class MenuItemOptionController {

    private final MenuItemOptionService service;

    public MenuItemOptionController(MenuItemOptionService service) {
        this.service = service;
    }

    @GetMapping
    public List<MenuItemOptionDto> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public MenuItemOptionDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<MenuItemOptionDto> create(@RequestBody MenuItemOptionDto dto) {
        MenuItemOptionDto created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/menu-item-options/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public MenuItemOptionDto update(@PathVariable UUID id, @RequestBody MenuItemOptionDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
