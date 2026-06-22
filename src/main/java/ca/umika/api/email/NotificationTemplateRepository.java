package ca.umika.api.email;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, UUID> {
}
