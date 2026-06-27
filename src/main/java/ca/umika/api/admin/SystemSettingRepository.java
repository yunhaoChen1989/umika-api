package ca.umika.api.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSettingEntity, UUID> {
    Optional<SystemSettingEntity> findBySettingKey(String settingKey);
    Optional<SystemSettingEntity> findBySettingGroupAndSettingKeyIgnoreCase(String settingGroup, String settingKey);
    Page<SystemSettingEntity> findBySettingGroupIgnoreCase(String settingGroup, Pageable pageable);
    List<SystemSettingEntity> findBySettingGroupIgnoreCase(String settingGroup);
    boolean existsBySettingGroupAndSettingKeyIgnoreCase(String settingGroup, String settingKey);
}
