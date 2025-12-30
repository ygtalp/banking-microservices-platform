package com.banking.notification.repository;

import com.banking.notification.model.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    Optional<UserPreference> findByUserId(String userId);

    Optional<UserPreference> findByEmail(String email);

    Optional<UserPreference> findByPhoneNumber(String phoneNumber);

    @Query("SELECT p FROM UserPreference p WHERE p.emailEnabled = true AND p.email IS NOT NULL")
    List<UserPreference> findUsersWithEmailEnabled();

    @Query("SELECT p FROM UserPreference p WHERE p.smsEnabled = true AND p.phoneNumber IS NOT NULL")
    List<UserPreference> findUsersWithSmsEnabled();

    @Query("SELECT p FROM UserPreference p WHERE p.pushEnabled = true AND p.deviceToken IS NOT NULL")
    List<UserPreference> findUsersWithPushEnabled();

    @Query("SELECT p FROM UserPreference p WHERE p.userId = :userId AND p.emailEnabled = true")
    Optional<UserPreference> findByUserIdWithEmailEnabled(@Param("userId") String userId);

    boolean existsByUserId(String userId);
}
