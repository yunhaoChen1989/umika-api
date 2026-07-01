package ca.umika.api.order;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    Page<OrderEntity> findByUserId(UUID userId, Pageable pageable);
    Page<OrderEntity> findByUserIdAndLocationIdIn(UUID userId, Collection<UUID> locationIds, Pageable pageable);
    Page<OrderEntity> findByLocationIdIn(Collection<UUID> locationIds, Pageable pageable);
    long countByUserIdAndStatusIn(UUID userId, Collection<String> statuses);
}
