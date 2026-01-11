package com.banking.loan.repository;

import com.banking.loan.model.Loan;
import com.banking.loan.model.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    Optional<Loan> findByLoanId(String loanId);
    List<Loan> findByCustomerId(String customerId);
    List<Loan> findByStatus(LoanStatus status);
    List<Loan> findByCustomerIdAndStatus(String customerId, LoanStatus status);
    List<Loan> findByAccountNumber(String accountNumber);
    Long countByCustomerId(String customerId);
}
