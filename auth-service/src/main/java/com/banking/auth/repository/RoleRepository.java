package com.banking.auth.repository;

import com.banking.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleName(String roleName);

    boolean existsByRoleName(String roleName);

    // Find roles by name pattern (for searching)
    @Query("SELECT r FROM Role r WHERE LOWER(r.roleName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Role> searchByName(@Param("searchTerm") String searchTerm);

    // Find multiple roles by their names (useful for assigning default roles)
    @Query("SELECT r FROM Role r WHERE r.roleName IN :roleNames")
    Set<Role> findByRoleNameIn(@Param("roleNames") List<String> roleNames);
}
