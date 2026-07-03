package ca.umika.api.menu;

import java.util.UUID;
import ca.umika.api.common.web.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class MenuItemImageService {

    private final MenuItemImageRepository repository;
    private final MenuItemImageMapper mapper;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemImageStorageService storageService;
    private final MenuAccessService menuAccessService;

    public MenuItemImageService(
            MenuItemImageRepository repository,
            MenuItemImageMapper mapper,
            MenuItemRepository menuItemRepository,
            MenuItemImageStorageService storageService,
            MenuAccessService menuAccessService
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.menuItemRepository = menuItemRepository;
        this.storageService = storageService;
        this.menuAccessService = menuAccessService;
    }

    @Transactional(readOnly = true)
    public Page<MenuItemImageDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public MenuItemImageDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItemImage not found: " + id));
    }

    public MenuItemImageDto create(Authentication authentication, MenuItemImageDto dto) {
        menuAccessService.assertWriteAccess(authentication, resolveMenuItemLocationId(dto.menuItemId()));
        MenuItemImageEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public MenuItemImageDto upload(Authentication authentication, UUID menuItemId, MultipartFile file, Boolean isPrimary, Integer sortOrder) {
        menuAccessService.assertWriteAccess(authentication, resolveMenuItemLocationId(menuItemId));
        String filename = storageService.store(file);
        String publicUrl = buildPublicPath(filename);

        MenuItemImageEntity entity = new MenuItemImageEntity();
        entity.setMenuItemId(menuItemId);
        entity.setImageUrl(publicUrl);
        entity.setIsPrimary(isPrimary != null ? isPrimary : Boolean.FALSE);
        entity.setSortOrder(sortOrder != null ? sortOrder : 0);
        entity.setId(null);

        return mapper.toDto(repository.save(entity));
    }

    public MenuItemImageDto update(Authentication authentication, UUID id, MenuItemImageDto dto) {
        MenuItemImageEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItemImage not found: " + id));
        menuAccessService.assertWriteAccess(authentication, resolveMenuItemLocationId(entity.getMenuItemId()));
        menuAccessService.assertWriteAccess(authentication, resolveMenuItemLocationId(dto.menuItemId()));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(Authentication authentication, UUID id) {
        MenuItemImageEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItemImage not found: " + id));
        menuAccessService.assertWriteAccess(authentication, resolveMenuItemLocationId(entity.getMenuItemId()));
        storageService.delete(entity.getImageUrl());
        repository.delete(entity);
    }

    private UUID resolveMenuItemLocationId(UUID menuItemId) {
        if (menuItemId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "menuItemId is required");
        }
        if (!menuItemRepository.existsMenuItemRowById(menuItemId)) {
            throw new ResourceNotFoundException("MenuItem not found: " + menuItemId);
        }
        return menuItemRepository.findLocationIdByMenuItemId(menuItemId).orElse(null);
    }

    private String buildPublicPath(String filename) {
        return "/uploads/menu-item-images/" + filename;
    }
}
