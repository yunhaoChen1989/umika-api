package ca.umika.api.store;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessHourRepository extends JpaRepository<BusinessHourEntity, UUID> {
    Page<BusinessHourEntity> findByLocationId(UUID locationId, Pageable pageable);
    Optional<BusinessHourEntity> findByLocationIdAndDayOfWeek(UUID locationId, Short dayOfWeek);
}
