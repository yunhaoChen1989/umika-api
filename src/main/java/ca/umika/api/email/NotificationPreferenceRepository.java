package ca.umika.api.email;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreferenceEntity, UUID> {
}
