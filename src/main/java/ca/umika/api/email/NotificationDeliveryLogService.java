package ca.umika.api.email;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationDeliveryLogService {

    private final NotificationDeliveryLogRepository repository;
    private final NotificationDeliveryLogMapper mapper;

    public NotificationDeliveryLogService(NotificationDeliveryLogRepository repository, NotificationDeliveryLogMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<NotificationDeliveryLogDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationDeliveryLogDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationDeliveryLog not found: " + id));
    }

    public NotificationDeliveryLogDto create(NotificationDeliveryLogDto dto) {
        NotificationDeliveryLogEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public NotificationDeliveryLogDto update(UUID id, NotificationDeliveryLogDto dto) {
        NotificationDeliveryLogEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationDeliveryLog not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("NotificationDeliveryLog not found: " + id);
        }
        repository.deleteById(id);
    }
}
