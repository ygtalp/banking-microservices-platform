package com.banking.auth.repository;

import com.banking.auth.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByResourceAndAction(String resource, String action);

    List<Permission> findByResource(String resource);

    List<Permission> findByAction(String action);

    boolean existsByResourceAndAction(String resource, String action);

    // Search permissions
    @Query("SELECT p FROM Permission p WHERE LOWER(p.resource) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(p.action) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Permission> searchPermissions(@Param("searchTerm") String searchTerm);
}
