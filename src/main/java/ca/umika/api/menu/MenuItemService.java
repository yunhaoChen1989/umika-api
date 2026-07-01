package ca.umika.api.menu;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MenuItemService {

    private final MenuItemRepository repository;
    private final MenuItemMapper mapper;
    private final MenuAccessService menuAccessService;

    public MenuItemService(MenuItemRepository repository, MenuItemMapper mapper, MenuAccessService menuAccessService) {
        this.repository = repository;
        this.mapper = mapper;
        this.menuAccessService = menuAccessService;
    }

    @Transactional(readOnly = true)
    public Page<MenuItemDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public MenuItemDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem not found: " + id));
    }

    public MenuItemDto create(Authentication authentication, MenuItemDto dto) {
        menuAccessService.assertWriteAccess(authentication, dto.locationId());
        MenuItemEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public MenuItemDto update(Authentication authentication, UUID id, MenuItemDto dto) {
        MenuItemEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem not found: " + id));
        menuAccessService.assertWriteAccess(authentication, entity.getLocationId());
        menuAccessService.assertWriteAccess(authentication, dto.locationId());
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(Authentication authentication, UUID id) {
        MenuItemEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem not found: " + id));
        menuAccessService.assertWriteAccess(authentication, entity.getLocationId());
        repository.delete(entity);
    }
}
