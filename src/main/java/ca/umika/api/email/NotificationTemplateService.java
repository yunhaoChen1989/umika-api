package ca.umika.api.email;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationTemplateService {

    private final NotificationTemplateRepository repository;
    private final NotificationTemplateMapper mapper;

    public NotificationTemplateService(NotificationTemplateRepository repository, NotificationTemplateMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationTemplateDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate not found: " + id));
    }

    public NotificationTemplateDto create(NotificationTemplateDto dto) {
        NotificationTemplateEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public NotificationTemplateDto update(UUID id, NotificationTemplateDto dto) {
        NotificationTemplateEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("NotificationTemplate not found: " + id);
        }
        repository.deleteById(id);
    }
}
