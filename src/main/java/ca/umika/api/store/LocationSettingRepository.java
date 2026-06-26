package ca.umika.api.store;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationSettingRepository extends JpaRepository<LocationSettingEntity, UUID> {
    Page<LocationSettingEntity> findByLocationId(UUID locationId, Pageable pageable);
    Optional<LocationSettingEntity> findByLocationIdAndSettingKeyIgnoreCase(UUID locationId, String settingKey);
    boolean existsByLocationIdAndSettingKeyIgnoreCase(UUID locationId, String settingKey);
}
