package ca.umika.api.auth;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCodeEntity, UUID> {

    Optional<EmailVerificationCodeEntity> findTopByEmailOrderByCreatedAtDesc(String email);

    long countByEmailAndCreatedAtAfter(String email, LocalDateTime createdAfter);

    long countByRequesterIpAndCreatedAtAfter(String requesterIp, LocalDateTime createdAfter);

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("update EmailVerificationCodeEntity code set code.attempts = code.attempts + 1 where code.id = :id")
    void incrementAttempts(@Param("id") UUID id);
}
