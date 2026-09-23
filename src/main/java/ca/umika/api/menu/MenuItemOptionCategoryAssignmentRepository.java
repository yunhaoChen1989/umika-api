package ca.umika.api.menu;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemOptionCategoryAssignmentRepository extends JpaRepository<MenuItemOptionCategoryAssignmentEntity, UUID> {
    List<MenuItemOptionCategoryAssignmentEntity> findByItemIdAndLocationIdIsNull(UUID itemId);
    List<MenuItemOptionCategoryAssignmentEntity> findByItemIdAndLocationId(UUID itemId, UUID locationId);
    List<MenuItemOptionCategoryAssignmentEntity> findByItemIdInAndLocationIdIsNull(List<UUID> itemIds);
    List<MenuItemOptionCategoryAssignmentEntity> findByItemIdInAndLocationId(List<UUID> itemIds, UUID locationId);
    void deleteByItemIdAndLocationIdIsNull(UUID itemId);
    void deleteByItemIdAndLocationId(UUID itemId, UUID locationId);
}
