package ca.umika.api.menu;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemOptionAssignmentSettingRepository extends JpaRepository<MenuItemOptionAssignmentSettingEntity, UUID> {
    Optional<MenuItemOptionAssignmentSettingEntity> findByItemIdAndLocationIdIsNull(UUID itemId);
    Optional<MenuItemOptionAssignmentSettingEntity> findByItemIdAndLocationId(UUID itemId, UUID locationId);
    List<MenuItemOptionAssignmentSettingEntity> findByItemIdInAndLocationIdIsNull(List<UUID> itemIds);
    List<MenuItemOptionAssignmentSettingEntity> findByItemIdInAndLocationId(List<UUID> itemIds, UUID locationId);
    void deleteByItemIdAndLocationIdIsNull(UUID itemId);
    void deleteByItemIdAndLocationId(UUID itemId, UUID locationId);
}
