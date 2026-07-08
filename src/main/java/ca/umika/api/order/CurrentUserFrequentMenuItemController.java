package ca.umika.api.order;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "CurrentUserFrequentMenuItems")
public class CurrentUserFrequentMenuItemController {

    private final CurrentUserFrequentMenuItemService service;

    public CurrentUserFrequentMenuItemController(CurrentUserFrequentMenuItemService service) {
        this.service = service;
    }

    @GetMapping("/frequent-menu-items")
    public List<CurrentUserFrequentMenuItemDto> find(
            Authentication authentication,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) UUID locationId
    ) {
        return service.find(authentication, limit, locationId);
    }
}
