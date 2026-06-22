package ca.umika.api.menu;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MenuItemOptionService {

    private final MenuItemOptionRepository repository;
    private final MenuItemOptionMapper mapper;

    public MenuItemOptionService(MenuItemOptionRepository repository, MenuItemOptionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<MenuItemOptionDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MenuItemOptionDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItemOption not found: " + id));
    }

    public MenuItemOptionDto create(MenuItemOptionDto dto) {
        MenuItemOptionEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public MenuItemOptionDto update(UUID id, MenuItemOptionDto dto) {
        MenuItemOptionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItemOption not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("MenuItemOption not found: " + id);
        }
        repository.deleteById(id);
    }
}
