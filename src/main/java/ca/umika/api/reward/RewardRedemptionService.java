package ca.umika.api.reward;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RewardRedemptionService {

    private final RewardRedemptionRepository repository;
    private final RewardRedemptionMapper mapper;

    public RewardRedemptionService(RewardRedemptionRepository repository, RewardRedemptionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<RewardRedemptionDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RewardRedemptionDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("RewardRedemption not found: " + id));
    }

    public RewardRedemptionDto create(RewardRedemptionDto dto) {
        RewardRedemptionEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public RewardRedemptionDto update(UUID id, RewardRedemptionDto dto) {
        RewardRedemptionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RewardRedemption not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("RewardRedemption not found: " + id);
        }
        repository.deleteById(id);
    }
}
