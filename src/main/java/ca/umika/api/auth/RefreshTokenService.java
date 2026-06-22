package ca.umika.api.auth;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final RefreshTokenMapper mapper;

    public RefreshTokenService(RefreshTokenRepository repository, RefreshTokenMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<RefreshTokenDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RefreshTokenDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("RefreshToken not found: " + id));
    }

    public RefreshTokenDto create(RefreshTokenDto dto) {
        RefreshTokenEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public RefreshTokenDto update(UUID id, RefreshTokenDto dto) {
        RefreshTokenEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RefreshToken not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("RefreshToken not found: " + id);
        }
        repository.deleteById(id);
    }
}
