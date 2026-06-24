package ca.umika.api.email;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;
    private final NotificationPreferenceMapper mapper;

    public NotificationPreferenceService(NotificationPreferenceRepository repository, NotificationPreferenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<NotificationPreferenceDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationPreference not found: " + id));
    }

    public NotificationPreferenceDto create(NotificationPreferenceDto dto) {
        NotificationPreferenceEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public NotificationPreferenceDto update(UUID id, NotificationPreferenceDto dto) {
        NotificationPreferenceEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationPreference not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("NotificationPreference not found: " + id);
        }
        repository.deleteById(id);
    }
}
