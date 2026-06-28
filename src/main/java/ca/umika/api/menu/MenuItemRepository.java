package ca.umika.api.menu;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItemEntity, UUID> {
    List<MenuItemEntity> findAllByIsDeletedFalseAndLocationIdIsNullOrderByDisplayOrderAscCreatedAtAsc();
    List<MenuItemEntity> findAllByIsDeletedFalseAndLocationIdOrderByDisplayOrderAscCreatedAtAsc(UUID locationId);
}
