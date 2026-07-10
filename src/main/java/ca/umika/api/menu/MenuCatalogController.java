package ca.umika.api.menu;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/menu-catalog", "/api/v1/manager/menu-catalog"})
@Tag(name = "MenuCatalog")
public class MenuCatalogController {

    private final MenuCatalogService service;

    public MenuCatalogController(MenuCatalogService service) {
        this.service = service;
    }

    @GetMapping
    public MenuCatalogResponseDto resolve(
            Authentication authentication,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) String locationCode
    ) {
        return service.resolve(authentication, locationId, locationCode);
    }
}
