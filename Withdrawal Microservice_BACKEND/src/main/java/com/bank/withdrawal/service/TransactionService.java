package com.bank.withdrawal.service;

import com.bank.withdrawal.entity.Account;
import com.bank.withdrawal.entity.AccountStatus;
import com.bank.withdrawal.entity.AccountTransaction;
import com.bank.withdrawal.entity.TxnStatus;
import com.bank.withdrawal.exception.ApiException;
import com.bank.withdrawal.gateway.PayoutResult;
import com.bank.withdrawal.repository.AccountRepository;
import com.bank.withdrawal.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * All short DB transactions live here. Nothing in this class calls Razorpay,
 * so row locks are held for milliseconds, never for a network call.
 */
@Service
public class TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    public record DebitResult(AccountTransaction txn, boolean created) {}

    private final AccountRepository accounts;
    private final TransactionRepository txns;
    private final BigDecimal maxPerTxn;

    public TransactionService(AccountRepository accounts, TransactionRepository txns,
                              @Value("${app.withdrawal.max-per-txn}") BigDecimal maxPerTxn) {
        this.accounts = accounts;
        this.txns = txns;
        this.maxPerTxn = maxPerTxn;
    }

    /** Phase 1: validate, lock, debit and record a PENDING transaction. Commits before Razorpay is called. */
    @Transactional
    public DebitResult debitAndCreatePending(UUID userId, UUID accountId, BigDecimal amount, String idempotencyKey) {
        Account acc = accounts.findByIdForUpdate(accountId)
                .orElseThrow(() -> reject(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found", accountId));

        if (!acc.getUserId().equals(userId)) {
            throw reject(HttpStatus.FORBIDDEN, "FORBIDDEN", "This account does not belong to you", accountId);
        }

        // Idempotency: same key => same result, no second debit. The account lock serialises concurrent duplicates.
        var existing = txns.findByAccountIdAndIdempotencyKey(accountId, idempotencyKey);
        if (existing.isPresent()) {
            if (existing.get().getAmount().compareTo(amount) != 0) {
                throw reject(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT",
                        "This request key was already used with a different amount", accountId);
            }
            return new DebitResult(existing.get(), false);
        }

        if (acc.getStatus() != AccountStatus.ACTIVE) {
            throw reject(HttpStatus.UNPROCESSABLE_ENTITY, "ACCOUNT_INACTIVE", "Account is not active", accountId);
        }
        if (acc.getRzpFundAccountId() == null) {
            throw reject(HttpStatus.UNPROCESSABLE_ENTITY, "NO_BENEFICIARY", "Add a bank account to withdraw to", accountId);
        }
        if (amount.compareTo(maxPerTxn) > 0) {
            throw reject(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Maximum per withdrawal is ₹" + maxPerTxn.toPlainString(), accountId);
        }
        if (acc.getBalance().compareTo(amount) < 0) {
            throw reject(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_FUNDS", "Insufficient balance", accountId);
        }

        Instant startOfDay = LocalDate.now(IST).atStartOfDay(IST).toInstant();
        BigDecimal usedToday = txns.sumSince(accountId, List.of(TxnStatus.PENDING, TxnStatus.SUCCESS), startOfDay);
        if (usedToday == null) usedToday = BigDecimal.ZERO;
        if (usedToday.add(amount).compareTo(acc.getDailyLimit()) > 0) {
            throw reject(HttpStatus.UNPROCESSABLE_ENTITY, "DAILY_LIMIT_EXCEEDED", "Daily withdrawal limit exceeded", accountId);
        }

        acc.setBalance(acc.getBalance().subtract(amount));   // managed entity: flushed on commit

        Instant now = Instant.now();
        AccountTransaction t = new AccountTransaction();
        t.setAccountId(accountId);
        t.setAmount(amount);
        t.setBalanceAfter(acc.getBalance());
        t.setStatus(TxnStatus.PENDING);
        t.setIdempotencyKey(idempotencyKey);
        t.setCreatedAt(now);
        t.setUpdatedAt(now);
        txns.save(t);
        log.info("withdrawal debited txn={} account={}", t.getId(), accountId);
        return new DebitResult(t, true);
    }

    @Transactional
    public void attachPayoutId(UUID txnId, String payoutId) {
        txns.findByIdForUpdate(txnId).ifPresent(t -> {
            if (t.getPayoutId() == null) {
                t.setPayoutId(payoutId);
                t.setUpdatedAt(Instant.now());
            }
        });
    }

    /** Idempotent: only PENDING moves to SUCCESS. */
    @Transactional
    public void markSuccess(UUID txnId) {
        txns.findByIdForUpdate(txnId).ifPresent(t -> {
            if (t.getStatus() == TxnStatus.PENDING) {
                t.setStatus(TxnStatus.SUCCESS);
                t.setUpdatedAt(Instant.now());
                log.info("withdrawal success txn={}", txnId);
            }
        });
    }

    /** Idempotent: refunds at most once (also covers "reversed" arriving after "processed"). */
    @Transactional
    public void markFailedAndRefund(UUID txnId, String reason) {
        AccountTransaction t = txns.findByIdForUpdate(txnId).orElse(null);
        if (t == null || t.isRefunded()) return;
        Account acc = accounts.findByIdForUpdate(t.getAccountId()).orElseThrow();
        acc.setBalance(acc.getBalance().add(t.getAmount()));
        t.setStatus(TxnStatus.FAILED);
        t.setRefunded(true);
        t.setFailureReason(reason == null ? null : reason.substring(0, Math.min(500, reason.length())));
        t.setUpdatedAt(Instant.now());
        log.warn("withdrawal failed + refunded txn={} reason={}", txnId, reason);
    }

    /** Apply a gateway status to our transaction. */
    public void applyResult(UUID txnId, PayoutResult r) {
        if (r.isProcessed()) markSuccess(txnId);
        else if (r.isFailed()) markFailedAndRefund(txnId, "Payout " + r.status());
        // queued / pending / processing: nothing to do yet
    }

    private ApiException reject(HttpStatus status, String code, String message, UUID accountId) {
        log.warn("withdrawal rejected account={} code={}", accountId, code);   // audit trail for failed attempts
        return new ApiException(status, code, message);
    }
}
