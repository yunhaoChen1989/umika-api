package ca.umika.api.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/permission-codes")
@Tag(name = "PermissionCode")
public class PermissionCodeController {

    private final PermissionCodeService service;

    public PermissionCodeController(PermissionCodeService service) {
        this.service = service;
    }

    @GetMapping
    public Page<PermissionCodeDto> findAll(
            Pageable pageable,
            @RequestParam(required = false) String permissionGroup,
            @RequestParam(required = false) Boolean activeOnly
    ) {
        return service.findAll(pageable, permissionGroup, activeOnly);
    }

    @GetMapping("/{code}")
    public PermissionCodeDto findByCode(@PathVariable String code) {
        return service.findByCode(code);
    }
}
