package ca.umika.api.email;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationRepository extends JpaRepository<UserNotificationEntity, UUID> {
}
