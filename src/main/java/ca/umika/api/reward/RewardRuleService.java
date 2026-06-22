package ca.umika.api.reward;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RewardRuleService {

    private final RewardRuleRepository repository;
    private final RewardRuleMapper mapper;

    public RewardRuleService(RewardRuleRepository repository, RewardRuleMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<RewardRuleDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RewardRuleDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("RewardRule not found: " + id));
    }

    public RewardRuleDto create(RewardRuleDto dto) {
        RewardRuleEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public RewardRuleDto update(UUID id, RewardRuleDto dto) {
        RewardRuleEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RewardRule not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("RewardRule not found: " + id);
        }
        repository.deleteById(id);
    }
}
