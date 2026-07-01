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
public class MenuItemOptionService {

    private final MenuItemOptionRepository repository;
    private final MenuItemOptionMapper mapper;
    private final MenuItemRepository menuItemRepository;
    private final MenuAccessService menuAccessService;

    public MenuItemOptionService(
            MenuItemOptionRepository repository,
            MenuItemOptionMapper mapper,
            MenuItemRepository menuItemRepository,
            MenuAccessService menuAccessService
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.menuItemRepository = menuItemRepository;
        this.menuAccessService = menuAccessService;
    }

    @Transactional(readOnly = true)
    public Page<MenuItemOptionDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public MenuItemOptionDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItemOption not found: " + id));
    }

    public MenuItemOptionDto create(Authentication authentication, MenuItemOptionDto dto) {
        menuAccessService.assertWriteAccess(authentication, resolveMenuItemLocationId(dto.itemId()));
        MenuItemOptionEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public MenuItemOptionDto update(Authentication authentication, UUID id, MenuItemOptionDto dto) {
        MenuItemOptionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItemOption not found: " + id));
        menuAccessService.assertWriteAccess(authentication, resolveMenuItemLocationId(entity.getItemId()));
        menuAccessService.assertWriteAccess(authentication, resolveMenuItemLocationId(dto.itemId()));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(Authentication authentication, UUID id) {
        MenuItemOptionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItemOption not found: " + id));
        menuAccessService.assertWriteAccess(authentication, resolveMenuItemLocationId(entity.getItemId()));
        repository.delete(entity);
    }

    private UUID resolveMenuItemLocationId(UUID menuItemId) {
        return menuItemRepository.findById(menuItemId)
                .map(MenuItemEntity::getLocationId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem not found: " + menuItemId));
    }
}
