package ca.umika.api.referral;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralRepository extends JpaRepository<ReferralEntity, UUID> {
    Optional<ReferralEntity> findFirstByReferredUserId(UUID referredUserId);
}
