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
public class UserNotificationService {

    private final UserNotificationRepository repository;
    private final UserNotificationMapper mapper;

    public UserNotificationService(UserNotificationRepository repository, UserNotificationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<UserNotificationDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public UserNotificationDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("UserNotification not found: " + id));
    }

    public UserNotificationDto create(UserNotificationDto dto) {
        UserNotificationEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public UserNotificationDto update(UUID id, UserNotificationDto dto) {
        UserNotificationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserNotification not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("UserNotification not found: " + id);
        }
        repository.deleteById(id);
    }
}
