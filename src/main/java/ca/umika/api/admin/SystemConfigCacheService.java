package ca.umika.api.admin;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SystemConfigCacheService {

    private final SystemConfigCacheRepository repository;
    private final SystemConfigCacheMapper mapper;

    public SystemConfigCacheService(SystemConfigCacheRepository repository, SystemConfigCacheMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<SystemConfigCacheDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SystemConfigCacheDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("SystemConfigCache not found: " + id));
    }

    public SystemConfigCacheDto create(SystemConfigCacheDto dto) {
        SystemConfigCacheEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public SystemConfigCacheDto update(UUID id, SystemConfigCacheDto dto) {
        SystemConfigCacheEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemConfigCache not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("SystemConfigCache not found: " + id);
        }
        repository.deleteById(id);
    }
}
