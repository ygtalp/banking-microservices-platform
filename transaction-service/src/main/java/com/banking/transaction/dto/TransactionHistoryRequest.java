package com.banking.transaction.dto;

import com.banking.transaction.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistoryRequest {
    private String accountNumber;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private TransactionType transactionType;
    private Integer page;
    private Integer size;
}
