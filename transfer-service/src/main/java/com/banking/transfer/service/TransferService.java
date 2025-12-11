package com.banking.transfer.service;

import com.banking.transfer.dto.TransferRequest;
import com.banking.transfer.dto.TransferResponse;
import com.banking.transfer.exception.DuplicateTransferException;
import com.banking.transfer.exception.TransferNotFoundException;
import com.banking.transfer.model.Transfer;
import com.banking.transfer.model.TransferStatus;
import com.banking.transfer.repository.TransferRepository;
import com.banking.transfer.saga.TransferSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final TransferRepository transferRepository;
    private final TransferSagaOrchestrator sagaOrchestrator;
    private final KafkaEventPublisher eventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String IDEMPOTENCY_KEY_PREFIX = "transfer:idempotency:";
    private static final long IDEMPOTENCY_TTL_HOURS = 24;

    @Transactional
    public TransferResponse initiateTransfer(TransferRequest request) {
        log.info("Initiating transfer from {} to {} - Amount: {} {}",
                request.getFromAccountNumber(), request.getToAccountNumber(),
                request.getAmount(), request.getCurrency());

        // Check for duplicate transfer using idempotency key
        if (request.getIdempotencyKey() != null) {
            Transfer existingTransfer = checkIdempotency(request.getIdempotencyKey());
            if (existingTransfer != null) {
                log.warn("Duplicate transfer detected with idempotency key: {}",
                        request.getIdempotencyKey());
                return mapToResponse(existingTransfer);
            }
        }

        // Create transfer entity
        Transfer transfer = Transfer.builder()
                .transferReference(generateTransferReference())
                .fromAccountNumber(request.getFromAccountNumber())
                .toAccountNumber(request.getToAccountNumber())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .description(request.getDescription())
                .status(TransferStatus.PENDING)
                .transferType(request.getTransferType())
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        // Save initial transfer
        transfer = transferRepository.save(transfer);

        // Store idempotency key in Redis
        if (request.getIdempotencyKey() != null) {
            storeIdempotencyKey(request.getIdempotencyKey(), transfer.getTransferReference());
        }

        // Publish initiated event
        eventPublisher.publishTransferInitiated(transfer);

        // Execute SAGA orchestration
        transfer = sagaOrchestrator.executeTransfer(transfer);

        // Publish completion/failure events
        if (transfer.getStatus() == TransferStatus.COMPLETED) {
            eventPublisher.publishTransferCompleted(transfer);
        } else if (transfer.getStatus() == TransferStatus.FAILED) {
            eventPublisher.publishTransferFailed(transfer);
        } else if (transfer.getStatus() == TransferStatus.COMPENSATED) {
            eventPublisher.publishTransferCompensated(transfer);
        }

        log.info("Transfer {} completed with status: {}",
                transfer.getTransferReference(), transfer.getStatus());

        return mapToResponse(transfer);
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransferByReference(String transferReference) {
        log.info("Fetching transfer by reference: {}", transferReference);

        Transfer transfer = transferRepository.findByTransferReference(transferReference)
                .orElseThrow(() -> new TransferNotFoundException(transferReference));

        return mapToResponse(transfer);
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> getTransfersByAccount(String accountNumber) {
        log.info("Fetching transfers for account: {}", accountNumber);

        List<Transfer> transfers = transferRepository.findByAccountNumber(accountNumber);

        return transfers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> getTransfersFrom(String accountNumber) {
        log.info("Fetching transfers from account: {}", accountNumber);

        List<Transfer> transfers = transferRepository
                .findByFromAccountNumberOrderByCreatedAtDesc(accountNumber);

        return transfers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> getTransfersTo(String accountNumber) {
        log.info("Fetching transfers to account: {}", accountNumber);

        List<Transfer> transfers = transferRepository
                .findByToAccountNumberOrderByCreatedAtDesc(accountNumber);

        return transfers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Transfer checkIdempotency(String idempotencyKey) {
        // Check Redis first (fast lookup)
        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        String transferReference = (String) redisTemplate.opsForValue().get(redisKey);

        if (transferReference != null) {
            return transferRepository.findByTransferReference(transferReference).orElse(null);
        }

        // Fallback to database
        return transferRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
    }

    private void storeIdempotencyKey(String idempotencyKey, String transferReference) {
        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(redisKey, transferReference,
                IDEMPOTENCY_TTL_HOURS, TimeUnit.HOURS);
    }

    private String generateTransferReference() {
        return "TXF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private TransferResponse mapToResponse(Transfer transfer) {
        return TransferResponse.builder()
                .id(transfer.getId())
                .transferReference(transfer.getTransferReference())
                .fromAccountNumber(transfer.getFromAccountNumber())
                .toAccountNumber(transfer.getToAccountNumber())
                .amount(transfer.getAmount())
                .currency(transfer.getCurrency())
                .description(transfer.getDescription())
                .status(transfer.getStatus())
                .transferType(transfer.getTransferType())
                .failureReason(transfer.getFailureReason())
                .initiatedAt(transfer.getInitiatedAt())
                .completedAt(transfer.getCompletedAt())
                .createdAt(transfer.getCreatedAt())
                .build();
    }
}