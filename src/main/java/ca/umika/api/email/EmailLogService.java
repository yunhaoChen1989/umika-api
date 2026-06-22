package ca.umika.api.email;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmailLogService {

    private final EmailLogRepository repository;
    private final EmailLogMapper mapper;

    public EmailLogService(EmailLogRepository repository, EmailLogMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<EmailLogDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmailLogDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("EmailLog not found: " + id));
    }

    public EmailLogDto create(EmailLogDto dto) {
        EmailLogEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public EmailLogDto update(UUID id, EmailLogDto dto) {
        EmailLogEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmailLog not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("EmailLog not found: " + id);
        }
        repository.deleteById(id);
    }
}
