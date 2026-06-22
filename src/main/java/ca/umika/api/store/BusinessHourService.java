package ca.umika.api.store;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BusinessHourService {

    private final BusinessHourRepository repository;
    private final BusinessHourMapper mapper;

    public BusinessHourService(BusinessHourRepository repository, BusinessHourMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<BusinessHourDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BusinessHourDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessHour not found: " + id));
    }

    public BusinessHourDto create(BusinessHourDto dto) {
        BusinessHourEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public BusinessHourDto update(UUID id, BusinessHourDto dto) {
        BusinessHourEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessHour not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("BusinessHour not found: " + id);
        }
        repository.deleteById(id);
    }
}
