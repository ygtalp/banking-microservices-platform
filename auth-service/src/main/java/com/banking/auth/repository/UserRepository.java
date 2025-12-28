package com.banking.auth.repository;

import com.banking.auth.model.User;
import com.banking.auth.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(String userId);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUserId(String userId);

    List<User> findByStatus(UserStatus status);

    List<User> findByAccountLocked(Boolean accountLocked);

    // Find users locked after a certain date
    @Query("SELECT u FROM User u WHERE u.accountLocked = true AND u.lockedAt > :since")
    List<User> findLockedUsersSince(@Param("since") LocalDateTime since);

    // Find users who haven't logged in since a certain date
    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :since OR u.lastLoginAt IS NULL")
    List<User> findInactiveUsersSince(@Param("since") LocalDateTime since);

    // Search users by name or email (case-insensitive)
    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchUsers(@Param("searchTerm") String searchTerm);

    // Count users by status
    long countByStatus(UserStatus status);

    // Count locked accounts
    long countByAccountLocked(Boolean accountLocked);
}
