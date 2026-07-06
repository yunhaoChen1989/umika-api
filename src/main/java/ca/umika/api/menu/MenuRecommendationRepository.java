package ca.umika.api.menu;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRecommendationRepository extends JpaRepository<MenuRecommendationEntity, UUID> {
    List<MenuRecommendationEntity> findByIsActiveTrueAndLocationIdIsNullOrderBySortOrderAscCreatedAtAsc();
    List<MenuRecommendationEntity> findByIsActiveTrueAndLocationIdOrderBySortOrderAscCreatedAtAsc(UUID locationId);
    Page<MenuRecommendationEntity> findByLocationId(UUID locationId, Pageable pageable);
    Page<MenuRecommendationEntity> findByLocationIdIsNull(Pageable pageable);
    List<MenuRecommendationEntity> findByLocationId(UUID locationId, Sort sort);
    List<MenuRecommendationEntity> findByLocationIdIsNull(Sort sort);
}
