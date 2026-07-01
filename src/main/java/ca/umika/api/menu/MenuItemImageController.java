package ca.umika.api.menu;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/menu-item-images")
@Tag(name = "MenuItemImage")
public class MenuItemImageController {

    private final MenuItemImageService service;

    public MenuItemImageController(MenuItemImageService service) {
        this.service = service;
    }

    @GetMapping
    public Page<MenuItemImageDto> findAll(Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    public MenuItemImageDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<MenuItemImageDto> create(Authentication authentication, @RequestBody MenuItemImageDto dto) {
        MenuItemImageDto created = service.create(authentication, dto);
        return ResponseEntity.created(URI.create("/api/v1/menu-item-images/" + created.id())).body(created);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MenuItemImageDto> upload(
            Authentication authentication,
            @RequestParam UUID menuItemId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Boolean isPrimary,
            @RequestParam(required = false) Integer sortOrder
    ) {
        String publicBaseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        MenuItemImageDto created = service.upload(authentication, menuItemId, file, isPrimary, sortOrder, publicBaseUrl);
        return ResponseEntity.created(URI.create("/api/v1/menu-item-images/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public MenuItemImageDto update(Authentication authentication, @PathVariable UUID id, @RequestBody MenuItemImageDto dto) {
        return service.update(authentication, id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(authentication, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/picture")
    public ResponseEntity<Void> deletePicture(Authentication authentication, @PathVariable UUID id) {
        service.delete(authentication, id);
        return ResponseEntity.noContent().build();
    }
}
