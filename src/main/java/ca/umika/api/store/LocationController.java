package ca.umika.api.store;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/locations")
@Tag(name = "Location")
public class LocationController {

    private final LocationService service;

    public LocationController(LocationService service) {
        this.service = service;
    }

    @GetMapping
    public Page<LocationDto> findAll(Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    public LocationDto findById(@PathVariable String id) {
        return service.findByLocationCode(id);
    }

    @GetMapping("/current")
    public LocationDto findCurrent(@RequestParam(required = false) String locationCode) {
        return service.findCurrent(locationCode);
    }

    @GetMapping("/resolve-id")
    public LocationIdDto resolveId(@RequestParam String locationCode) {
        return service.resolveIdByLocationCode(locationCode);
    }

    @PostMapping
    public ResponseEntity<LocationDto> create(@RequestBody LocationDto dto) {
        LocationDto created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/locations/" + created.locationCode())).body(created);
    }

    @PutMapping("/{id}")
    public LocationDto update(@PathVariable String id, @RequestBody LocationDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
