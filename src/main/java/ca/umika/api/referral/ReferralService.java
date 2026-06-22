package ca.umika.api.referral;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReferralService {

    private final ReferralRepository repository;
    private final ReferralMapper mapper;

    public ReferralService(ReferralRepository repository, ReferralMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ReferralDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReferralDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Referral not found: " + id));
    }

    public ReferralDto create(ReferralDto dto) {
        ReferralEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public ReferralDto update(UUID id, ReferralDto dto) {
        ReferralEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Referral not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Referral not found: " + id);
        }
        repository.deleteById(id);
    }
}
