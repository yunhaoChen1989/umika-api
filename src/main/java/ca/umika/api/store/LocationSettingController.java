package ca.umika.api.store;

import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/location-settings")
@Tag(name = "LocationSetting")
public class LocationSettingController {

    private final LocationSettingService service;

    public LocationSettingController(LocationSettingService service) {
        this.service = service;
    }

    @GetMapping
    public Page<LocationSettingDto> findAll(
            Authentication authentication,
            Pageable pageable,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) String settingGroup
    ) {
        return service.findAll(authentication, pageable, locationId, settingGroup);
    }

    @GetMapping("/{id}")
    public LocationSettingDto findById(Authentication authentication, @PathVariable UUID id) {
        return service.findById(authentication, id);
    }

    @PostMapping
    public ResponseEntity<LocationSettingDto> create(Authentication authentication, @RequestBody LocationSettingDto dto) {
        LocationSettingDto created = service.create(authentication, dto);
        return ResponseEntity.created(URI.create("/api/v1/location-settings/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public LocationSettingDto update(Authentication authentication, @PathVariable UUID id, @RequestBody LocationSettingDto dto) {
        return service.update(authentication, id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(authentication, id);
        return ResponseEntity.noContent().build();
    }
}
