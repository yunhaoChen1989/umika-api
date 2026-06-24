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
public class SystemSettingService {

    private final SystemSettingRepository repository;
    private final SystemSettingMapper mapper;

    public SystemSettingService(SystemSettingRepository repository, SystemSettingMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<SystemSettingDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public SystemSettingDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("SystemSetting not found: " + id));
    }

    public SystemSettingDto create(SystemSettingDto dto) {
        SystemSettingEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public SystemSettingDto update(UUID id, SystemSettingDto dto) {
        SystemSettingEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemSetting not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("SystemSetting not found: " + id);
        }
        repository.deleteById(id);
    }
}
