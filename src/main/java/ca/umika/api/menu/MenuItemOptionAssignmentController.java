package ca.umika.api.menu;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/manager/menu-items/{itemId}/option-categories")
public class MenuItemOptionAssignmentController {
    private final MenuOptionAssignmentService service;

    public MenuItemOptionAssignmentController(MenuOptionAssignmentService service) { this.service = service; }

    @GetMapping
    public List<MenuCatalogOptionGroupDto> get(Authentication authentication, @PathVariable UUID itemId,
            @RequestParam(required = false) UUID locationId) {
        return service.getAssignments(authentication, itemId, locationId);
    }

    @PutMapping
    public List<MenuCatalogOptionGroupDto> replace(Authentication authentication, @PathVariable UUID itemId,
            @RequestParam(required = false) UUID locationId, @RequestBody MenuItemOptionAssignmentRequest request) {
        return service.replaceAssignments(authentication, itemId, locationId, request);
    }
}
