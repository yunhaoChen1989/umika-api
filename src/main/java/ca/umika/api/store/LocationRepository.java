package ca.umika.api.store;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<LocationEntity, UUID> {
    Optional<LocationEntity> findFirstByOrderByCreatedAtAsc();
    Optional<LocationEntity> findByLocationCodeIgnoreCase(String locationCode);
    boolean existsByLocationCodeIgnoreCase(String locationCode);
}
