package ca.umika.api.reward;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RewardTransactionService {

    private final RewardTransactionRepository repository;
    private final RewardTransactionMapper mapper;

    public RewardTransactionService(RewardTransactionRepository repository, RewardTransactionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<RewardTransactionDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public RewardTransactionDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("RewardTransaction not found: " + id));
    }

    public RewardTransactionDto create(RewardTransactionDto dto) {
        RewardTransactionEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public RewardTransactionDto update(UUID id, RewardTransactionDto dto) {
        RewardTransactionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RewardTransaction not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("RewardTransaction not found: " + id);
        }
        repository.deleteById(id);
    }
}
