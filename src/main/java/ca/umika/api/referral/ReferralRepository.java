package ca.umika.api.referral;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralRepository extends JpaRepository<ReferralEntity, UUID> {
}
