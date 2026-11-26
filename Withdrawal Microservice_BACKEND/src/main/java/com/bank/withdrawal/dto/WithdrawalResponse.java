package com.bank.withdrawal.dto;

import com.bank.withdrawal.entity.AccountTransaction;
import com.bank.withdrawal.entity.TxnStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WithdrawalResponse(UUID transactionId, UUID accountId, BigDecimal amount, BigDecimal balanceAfter,
                                 TxnStatus status, String failureReason, Instant createdAt) {
    public static WithdrawalResponse from(AccountTransaction t) {
        return new WithdrawalResponse(t.getId(), t.getAccountId(), t.getAmount(), t.getBalanceAfter(),
                t.getStatus(), t.getFailureReason(), t.getCreatedAt());
    }
}
