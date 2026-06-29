package ca.umika.api.menu;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationMenuOverrideRepository extends JpaRepository<LocationMenuOverrideEntity, UUID> {
    List<LocationMenuOverrideEntity> findByLocationId(UUID locationId);
    Page<LocationMenuOverrideEntity> findByLocationId(UUID locationId, Pageable pageable);

    Optional<LocationMenuOverrideEntity> findByLocationIdAndTargetTypeAndTargetId(UUID locationId, String targetType, UUID targetId);
}
