package ca.umika.api.store;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationSettingRepository extends JpaRepository<LocationSettingEntity, UUID> {
    Page<LocationSettingEntity> findByLocationId(UUID locationId, Pageable pageable);
    Page<LocationSettingEntity> findByLocationIdIn(Collection<UUID> locationIds, Pageable pageable);
    Page<LocationSettingEntity> findByLocationIdAndSettingGroupIgnoreCase(UUID locationId, String settingGroup, Pageable pageable);
    Page<LocationSettingEntity> findByLocationIdInAndSettingGroupIgnoreCase(Collection<UUID> locationIds, String settingGroup, Pageable pageable);
    Page<LocationSettingEntity> findBySettingGroupIgnoreCase(String settingGroup, Pageable pageable);
    Optional<LocationSettingEntity> findByLocationIdAndSettingGroupIgnoreCaseAndSettingKeyIgnoreCase(UUID locationId, String settingGroup, String settingKey);
    Optional<LocationSettingEntity> findByLocationIdAndSettingKeyIgnoreCase(UUID locationId, String settingKey);
    boolean existsByLocationIdAndSettingKeyIgnoreCase(UUID locationId, String settingKey);
}
