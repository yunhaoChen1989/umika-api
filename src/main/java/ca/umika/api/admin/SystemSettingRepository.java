package ca.umika.api.admin;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSettingEntity, UUID> {
    Optional<SystemSettingEntity> findBySettingKey(String settingKey);
}
