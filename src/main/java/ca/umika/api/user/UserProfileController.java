package ca.umika.api.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user-profiles")
@Tag(name = "UserProfile")
public class UserProfileController {

    private final UserProfileService service;

    public UserProfileController(UserProfileService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserProfileDto> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public UserProfileDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<UserProfileDto> create(@RequestBody UserProfileDto dto) {
        UserProfileDto created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/user-profiles/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public UserProfileDto update(@PathVariable UUID id, @RequestBody UserProfileDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
