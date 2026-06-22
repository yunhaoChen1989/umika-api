package ca.umika.api.store;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LocationService {

    private final LocationRepository repository;
    private final LocationMapper mapper;

    public LocationService(LocationRepository repository, LocationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<LocationDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + id));
    }

    public LocationDto create(LocationDto dto) {
        LocationEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public LocationDto update(UUID id, LocationDto dto) {
        LocationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Location not found: " + id);
        }
        repository.deleteById(id);
    }
}
