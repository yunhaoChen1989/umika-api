package ca.umika.api.reward;

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
@RequestMapping("/api/v1/reward-wallets")
@Tag(name = "RewardWallet")
public class RewardWalletController {

    private final RewardWalletService service;

    public RewardWalletController(RewardWalletService service) {
        this.service = service;
    }

    @GetMapping
    public List<RewardWalletDto> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public RewardWalletDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<RewardWalletDto> create(@RequestBody RewardWalletDto dto) {
        RewardWalletDto created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/reward-wallets/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public RewardWalletDto update(@PathVariable UUID id, @RequestBody RewardWalletDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
