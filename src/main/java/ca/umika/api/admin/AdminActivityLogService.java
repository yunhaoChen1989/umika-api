package ca.umika.api.admin;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminActivityLogService {

    private final AdminActivityLogRepository repository;
    private final AdminActivityLogMapper mapper;

    public AdminActivityLogService(AdminActivityLogRepository repository, AdminActivityLogMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<AdminActivityLogDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminActivityLogDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("AdminActivityLog not found: " + id));
    }

    public AdminActivityLogDto create(AdminActivityLogDto dto) {
        AdminActivityLogEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public AdminActivityLogDto update(UUID id, AdminActivityLogDto dto) {
        AdminActivityLogEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdminActivityLog not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("AdminActivityLog not found: " + id);
        }
        repository.deleteById(id);
    }
}
