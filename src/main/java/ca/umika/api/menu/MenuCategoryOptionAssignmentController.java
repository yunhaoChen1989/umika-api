package ca.umika.api.menu;

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
@RequestMapping("/api/v1/manager/menu-categories/{menuCategoryId}/option-categories")
public class MenuCategoryOptionAssignmentController {
    private final MenuOptionAssignmentService service;

    public MenuCategoryOptionAssignmentController(MenuOptionAssignmentService service) {
        this.service = service;
    }

    @GetMapping
    public MenuOptionAssignmentResponse get(Authentication authentication, @PathVariable UUID menuCategoryId,
            @RequestParam(required = false) UUID locationId) {
        return service.getMenuCategoryAssignments(authentication, menuCategoryId, locationId);
    }

    @PutMapping
    public MenuOptionAssignmentResponse replace(Authentication authentication, @PathVariable UUID menuCategoryId,
            @RequestParam(required = false) UUID locationId, @RequestBody MenuItemOptionAssignmentRequest request) {
        return service.replaceMenuCategoryAssignments(authentication, menuCategoryId, locationId, request);
    }
}
