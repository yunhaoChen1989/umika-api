package ca.umika.api.menu;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MenuItemService {

    private final MenuItemRepository repository;
    private final MenuItemMapper mapper;

    public MenuItemService(MenuItemRepository repository, MenuItemMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<MenuItemDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MenuItemDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem not found: " + id));
    }

    public MenuItemDto create(MenuItemDto dto) {
        MenuItemEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public MenuItemDto update(UUID id, MenuItemDto dto) {
        MenuItemEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("MenuItem not found: " + id);
        }
        repository.deleteById(id);
    }
}
