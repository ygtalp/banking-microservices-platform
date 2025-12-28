package com.banking.customer.dto.response;

import com.banking.customer.model.CustomerStatus;
import com.banking.customer.model.RiskLevel;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerResponse {

    private Long id;
    private String customerId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String nationalId; // Will be masked in service layer
    private String address;
    private String city;
    private String country;
    private String postalCode;
    private CustomerStatus status;
    private String statusReason;
    private RiskLevel riskLevel;
    private LocalDateTime verifiedAt;
    private String verifiedBy;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
