package ca.umika.api.email;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryLogRepository extends JpaRepository<NotificationDeliveryLogEntity, UUID> {
}
