package com.banking.auth.repository;

import com.banking.auth.model.MfaSecret;
import com.banking.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for MFA Secret operations
 */
@Repository
public interface MfaSecretRepository extends JpaRepository<MfaSecret, Long> {

    /**
     * Find MFA secret by user
     */
    Optional<MfaSecret> findByUser(User user);

    /**
     * Find MFA secret by user ID
     */
    Optional<MfaSecret> findByUserId(Long userId);

    /**
     * Check if user has MFA enabled
     */
    boolean existsByUserAndEnabledTrue(User user);

    /**
     * Delete MFA secret by user
     */
    void deleteByUser(User user);
}
