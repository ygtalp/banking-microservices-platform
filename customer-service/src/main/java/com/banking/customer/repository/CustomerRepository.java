package com.banking.customer.repository;

import com.banking.customer.model.Customer;
import com.banking.customer.model.CustomerStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerId(String customerId);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByNationalId(String nationalId);

    boolean existsByEmail(String email);

    boolean existsByNationalId(String nationalId);

    boolean existsByCustomerId(String customerId);

    List<Customer> findByStatus(CustomerStatus status);

    List<Customer> findByStatusOrderByCreatedAtDesc(CustomerStatus status);

    @Query("SELECT c FROM Customer c WHERE c.status IN :statuses ORDER BY c.createdAt DESC")
    List<Customer> findByStatusIn(@Param("statuses") List<CustomerStatus> statuses);

    // For optimistic locking - use when updating customer
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT c FROM Customer c WHERE c.customerId = :customerId")
    Optional<Customer> findByCustomerIdForUpdate(@Param("customerId") String customerId);

    // Search customers by name (case-insensitive)
    @Query("SELECT c FROM Customer c WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Customer> searchByName(@Param("searchTerm") String searchTerm);

    // Count customers by status
    long countByStatus(CustomerStatus status);
}
