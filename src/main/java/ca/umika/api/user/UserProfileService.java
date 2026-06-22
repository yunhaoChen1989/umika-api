package ca.umika.api.user;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserProfileService {

    private final UserProfileRepository repository;
    private final UserProfileMapper mapper;

    public UserProfileService(UserProfileRepository repository, UserProfileMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<UserProfileDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserProfileDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile not found: " + id));
    }

    public UserProfileDto create(UserProfileDto dto) {
        UserProfileEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public UserProfileDto update(UUID id, UserProfileDto dto) {
        UserProfileEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("UserProfile not found: " + id);
        }
        repository.deleteById(id);
    }
}
