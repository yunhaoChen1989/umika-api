package ca.umika.api.reward;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RewardWalletService {

    private final RewardWalletRepository repository;
    private final RewardWalletMapper mapper;

    public RewardWalletService(RewardWalletRepository repository, RewardWalletMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<RewardWalletDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RewardWalletDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("RewardWallet not found: " + id));
    }

    public RewardWalletDto create(RewardWalletDto dto) {
        RewardWalletEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public RewardWalletDto update(UUID id, RewardWalletDto dto) {
        RewardWalletEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RewardWallet not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("RewardWallet not found: " + id);
        }
        repository.deleteById(id);
    }
}
