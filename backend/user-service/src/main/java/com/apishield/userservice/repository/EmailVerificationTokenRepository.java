package com.apishield.userservice.repository;

import com.apishield.userservice.entity.EmailVerificationToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM EmailVerificationToken t WHERE t.tokenHash = :tokenHash")
    Optional<EmailVerificationToken> findByTokenHashWithLock(@Param("tokenHash") String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE EmailVerificationToken t SET t.usedAt = :now WHERE t.user.id = :userId AND t.usedAt IS NULL")
    void invalidateAllActiveTokensForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
