package com.cms.repository;

import com.cms.entity.UserEmailPreference;
import com.cms.entity.UserEmailPreference.DigestFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserEmailPreferenceRepository extends JpaRepository<UserEmailPreference, Long> {

    Optional<UserEmailPreference> findByUserId(Long userId);

    @Query("SELECT p FROM UserEmailPreference p JOIN FETCH p.user WHERE p.digestEnabled = true " +
           "AND p.digestFrequency = :frequency " +
           "AND (p.lastDigestSentAt IS NULL OR p.lastDigestSentAt < :cutoff)")
    List<UserEmailPreference> findDueForDigest(
            @Param("frequency") DigestFrequency frequency,
            @Param("cutoff") Instant cutoff);
}
