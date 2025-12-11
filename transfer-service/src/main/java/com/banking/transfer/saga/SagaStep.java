package com.banking.transfer.saga;

import com.banking.transfer.model.Transfer;

public interface SagaStep {
    /**
     * Execute the saga step
     * @param transfer The transfer entity
     * @return true if successful, false otherwise
     */
    boolean execute(Transfer transfer);

    /**
     * Compensate (rollback) the saga step
     * @param transfer The transfer entity
     * @return true if compensation successful, false otherwise
     */
    boolean compensate(Transfer transfer);

    /**
     * Get the step name for logging
     * @return Step name
     */
    String getStepName();
}