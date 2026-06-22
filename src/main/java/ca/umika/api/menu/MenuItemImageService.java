package ca.umika.api.menu;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MenuItemImageService {

    private final MenuItemImageRepository repository;
    private final MenuItemImageMapper mapper;

    public MenuItemImageService(MenuItemImageRepository repository, MenuItemImageMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<MenuItemImageDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MenuItemImageDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItemImage not found: " + id));
    }

    public MenuItemImageDto create(MenuItemImageDto dto) {
        MenuItemImageEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public MenuItemImageDto update(UUID id, MenuItemImageDto dto) {
        MenuItemImageEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItemImage not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("MenuItemImage not found: " + id);
        }
        repository.deleteById(id);
    }
}
