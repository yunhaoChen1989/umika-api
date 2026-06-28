package ca.umika.api.menu;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/location-menu-overrides")
@Tag(name = "LocationMenuOverride")
public class LocationMenuOverrideController {

    private final LocationMenuOverrideService service;

    public LocationMenuOverrideController(LocationMenuOverrideService service) {
        this.service = service;
    }

    @GetMapping
    public Page<LocationMenuOverrideDto> findAll(Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    public LocationMenuOverrideDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<LocationMenuOverrideDto> create(@RequestBody LocationMenuOverrideDto dto) {
        LocationMenuOverrideDto created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/location-menu-overrides/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public LocationMenuOverrideDto update(@PathVariable UUID id, @RequestBody LocationMenuOverrideDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
