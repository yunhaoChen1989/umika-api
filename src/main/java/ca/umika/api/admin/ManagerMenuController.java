package ca.umika.api.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/manager")
@Tag(name = "ManagerMenu")
public class ManagerMenuController {

    private final ManagerMenuService service;

    public ManagerMenuController(ManagerMenuService service) {
        this.service = service;
    }

    @GetMapping("/menus")
    public List<ManagerMenuNodeDto> menus(Authentication authentication) {
        return service.getMenus(authentication);
    }
}
