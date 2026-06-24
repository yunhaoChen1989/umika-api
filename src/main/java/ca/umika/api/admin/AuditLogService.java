package ca.umika.api.admin;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository repository;
    private final AuditLogMapper mapper;

    public AuditLogService(AuditLogRepository repository, AuditLogMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public AuditLogDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog not found: " + id));
    }

    public AuditLogDto create(AuditLogDto dto) {
        AuditLogEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public AuditLogDto update(UUID id, AuditLogDto dto) {
        AuditLogEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("AuditLog not found: " + id);
        }
        repository.deleteById(id);
    }
}
