package com.cms.repository;

import com.cms.entity.UserAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAlertRepository extends JpaRepository<UserAlert, Long> {

    List<UserAlert> findByUserIdAndDismissedFalseOrderByCreatedAtDesc(Long userId);

    Optional<UserAlert> findByUuid(String uuid);

    long countByUserIdAndDismissedFalse(Long userId);
}
