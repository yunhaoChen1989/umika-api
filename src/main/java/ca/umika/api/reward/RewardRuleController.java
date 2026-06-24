package ca.umika.api.reward;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/v1/reward-rules")
@Tag(name = "RewardRule")
public class RewardRuleController {

    private final RewardRuleService service;

    public RewardRuleController(RewardRuleService service) {
        this.service = service;
    }

    @GetMapping
    public Page<RewardRuleDto> findAll(Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    public RewardRuleDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<RewardRuleDto> create(@RequestBody RewardRuleDto dto) {
        RewardRuleDto created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/reward-rules/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public RewardRuleDto update(@PathVariable UUID id, @RequestBody RewardRuleDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
