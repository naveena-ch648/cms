package com.cms.repository;

import com.cms.entity.UserTwoFactor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTwoFactorRepository extends JpaRepository<UserTwoFactor, Long> {

    Optional<UserTwoFactor> findByUserId(Long userId);
}
