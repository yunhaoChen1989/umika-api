package ca.umika.api.admin;

import ca.umika.api.common.web.ResourceNotFoundException;
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
    public Page<SystemSettingDto> findAll(Pageable pageable, String settingGroup) {
        if (settingGroup != null && !settingGroup.isBlank()) {
            return repository.findBySettingGroupIgnoreCase(normalizeSettingGroup(settingGroup), pageable).map(mapper::toDto);
        }
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public SystemSettingDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("SystemSetting not found: " + id));
    }

    public SystemSettingDto create(SystemSettingDto dto) {
        String settingGroup = normalizeSettingGroup(dto.settingGroup());
        ensureUnique(settingGroup, dto.settingKey(), null);
        SystemSettingEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        entity.setSettingGroup(settingGroup);
        return mapper.toDto(repository.save(entity));
    }

    public SystemSettingDto update(UUID id, SystemSettingDto dto) {
        SystemSettingEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemSetting not found: " + id));
        String settingGroup = normalizeSettingGroup(dto.settingGroup());
        ensureUnique(settingGroup, dto.settingKey(), id);
        mapper.updateEntity(entity, dto);
        entity.setSettingGroup(settingGroup);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("SystemSetting not found: " + id);
        }
        repository.deleteById(id);
    }

    private void ensureUnique(String settingGroup, String settingKey, UUID currentId) {
        repository.findBySettingGroupAndSettingKeyIgnoreCase(settingGroup, settingKey)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "System setting already exists for group " + settingGroup + " and key " + settingKey
                    );
                });
    }

    private String normalizeSettingGroup(String settingGroup) {
        if (settingGroup == null || settingGroup.isBlank()) {
            return "GENERAL";
        }
        return settingGroup.trim().toUpperCase();
    }
}
