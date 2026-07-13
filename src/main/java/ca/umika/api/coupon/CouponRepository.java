package ca.umika.api.coupon;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<CouponEntity, UUID> {
    Optional<CouponEntity> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    Page<CouponEntity> findByLocationId(UUID locationId, Pageable pageable);
    Page<CouponEntity> findByLocationIdIsNull(Pageable pageable);
}
