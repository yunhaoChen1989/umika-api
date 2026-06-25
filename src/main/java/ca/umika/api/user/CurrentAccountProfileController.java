package ca.umika.api.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "CurrentAccount")
public class CurrentAccountProfileController {

    private final CurrentAccountProfileService service;

    public CurrentAccountProfileController(CurrentAccountProfileService service) {
        this.service = service;
    }

    @GetMapping("/profile")
    public CurrentAccountProfileDto profile(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return service.getByEmail(authentication.getName());
    }

    @PatchMapping("/profile")
    public CurrentAccountProfileDto update(
            Authentication authentication,
            @RequestBody CurrentAccountProfileUpdateRequest request
    ) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return service.updateByEmail(authentication.getName(), request);
    }
}
