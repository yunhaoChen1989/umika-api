package ca.umika.api.menu;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/menu-recommendations", "/api/v1/manager/menu-recommendations"})
@Tag(name = "MenuRecommendation")
public class MenuRecommendationController {

    private final MenuRecommendationService service;

    public MenuRecommendationController(MenuRecommendationService service) {
        this.service = service;
    }

    @GetMapping
    public List<MenuRecommendationResponse> findPublic(
            Authentication authentication,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) Integer limit
    ) {
        return service.findPublic(authentication, locationId, limit);
    }

    @GetMapping("/manage")
    public Page<MenuRecommendationManageResponse> findManage(
            Authentication authentication,
            Pageable pageable,
            @RequestParam(required = false) UUID locationId
    ) {
        return service.findManage(authentication, pageable, locationId);
    }

    @GetMapping("/{id}")
    public MenuRecommendationDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<MenuRecommendationDto> create(Authentication authentication, @RequestBody MenuRecommendationDto dto) {
        MenuRecommendationDto created = service.create(authentication, dto);
        return ResponseEntity.created(URI.create("/api/v1/menu-recommendations/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public MenuRecommendationDto update(Authentication authentication, @PathVariable UUID id, @RequestBody MenuRecommendationDto dto) {
        return service.update(authentication, id, dto);
    }

    @PatchMapping("/location-visibility")
    public MenuRecommendationVisibilityResponse updateLocationVisibility(
            Authentication authentication,
            @RequestBody MenuRecommendationVisibilityRequest request
    ) {
        return service.updateLocationVisibility(authentication, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(authentication, id);
        return ResponseEntity.noContent().build();
    }
}
