package ca.umika.api.menu;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/location-menu-overrides")
@Tag(name = "LocationMenuOverride")
public class LocationMenuOverrideController {

    private final LocationMenuOverrideService service;

    public LocationMenuOverrideController(LocationMenuOverrideService service) {
        this.service = service;
    }

    @GetMapping
    public Page<LocationMenuOverrideDto> findAll(
            Authentication authentication,
            Pageable pageable,
            @RequestParam(required = false) UUID locationId
    ) {
        return service.findAll(authentication, pageable, locationId);
    }

    @GetMapping("/{id}")
    public LocationMenuOverrideDto findById(Authentication authentication, @PathVariable UUID id) {
        return service.findById(authentication, id);
    }

    @PostMapping
    public ResponseEntity<LocationMenuOverrideDto> create(Authentication authentication, @RequestBody LocationMenuOverrideDto dto) {
        LocationMenuOverrideDto created = service.create(authentication, dto);
        return ResponseEntity.created(URI.create("/api/v1/location-menu-overrides/" + created.id())).body(created);
    }

    @PutMapping("/by-target")
    public LocationMenuOverrideDto upsert(Authentication authentication, @RequestBody LocationMenuOverrideDto dto) {
        return service.upsert(authentication, dto);
    }

    @PostMapping(value = "/item-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LocationMenuOverrideDto uploadItemOverrideImage(
            Authentication authentication,
            @RequestParam UUID locationId,
            @RequestParam UUID menuItemId,
            @RequestPart("file") MultipartFile file
    ) {
        return service.uploadItemOverrideImage(authentication, locationId, menuItemId, file);
    }

    @PutMapping("/{id}")
    public LocationMenuOverrideDto update(Authentication authentication, @PathVariable UUID id, @RequestBody LocationMenuOverrideDto dto) {
        return service.update(authentication, id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(authentication, id);
        return ResponseEntity.noContent().build();
    }
}
