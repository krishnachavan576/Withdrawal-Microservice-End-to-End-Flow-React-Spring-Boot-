package com.bank.withdrawal.service;

import com.bank.withdrawal.entity.Account;
import com.bank.withdrawal.entity.AccountTransaction;
import com.bank.withdrawal.exception.ApiException;
import com.bank.withdrawal.gateway.PayoutGateway;
import com.bank.withdrawal.gateway.PayoutGatewayException;
import com.bank.withdrawal.gateway.PayoutResult;
import com.bank.withdrawal.repository.AccountRepository;
import com.bank.withdrawal.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Orchestrates: (1) debit in DB  ->  (2) call Razorpay  ->  (3) webhook / reconciliation settles the result. */
@Service
public class WithdrawalService {
    private static final Logger log = LoggerFactory.getLogger(WithdrawalService.class);

    public record Result(AccountTransaction txn, boolean created) {}

    private final TransactionService txnService;
    private final PayoutGateway gateway;
    private final AccountRepository accounts;
    private final TransactionRepository txns;

    public WithdrawalService(TransactionService txnService, PayoutGateway gateway,
                             AccountRepository accounts, TransactionRepository txns) {
        this.txnService = txnService;
        this.gateway = gateway;
        this.accounts = accounts;
        this.txns = txns;
    }

    public Result withdraw(UUID userId, UUID accountId, BigDecimal amount, String idempotencyKey) {
        var debit = txnService.debitAndCreatePending(userId, accountId, amount, idempotencyKey);   // commits here
        AccountTransaction txn = debit.txn();
        if (!debit.created()) return new Result(txn, false);   // duplicate request: return the original outcome

        Account acc = accounts.findById(accountId).orElseThrow();
        try {
            // Our txn id is the Razorpay idempotency key => a retry can never create a second payout.
            PayoutResult p = gateway.createPayout(acc.getRzpFundAccountId(), amount,
                    txn.getId().toString(), txn.getId().toString());
            txnService.attachPayoutId(txn.getId(), p.payoutId());
            if (p.isFailed()) {
                txnService.markFailedAndRefund(txn.getId(), "Payout " + p.status());
                throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYOUT_FAILED", "The bank transfer failed. Your money was returned.");
            }
            if (p.isProcessed()) txnService.markSuccess(txn.getId());
        } catch (PayoutGatewayException.Rejected e) {
            txnService.markFailedAndRefund(txn.getId(), e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYOUT_FAILED", "The bank transfer was rejected. Your money was returned.");
        } catch (PayoutGatewayException.Unknown e) {
            // Result unknown (timeout etc). Do NOT refund. Stay PENDING; webhook or reconciliation decides.
            log.warn("payout status unknown txn={}: {}", txn.getId(), e.getMessage());
        }
        return new Result(txns.findById(txn.getId()).orElseThrow(), true);
    }

    public AccountTransaction getForUser(UUID userId, UUID txnId) {
        AccountTransaction t = txns.findById(txnId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Transaction not found"));
        requireOwner(userId, t.getAccountId());
        return t;
    }

    public List<AccountTransaction> listForUser(UUID userId, UUID accountId) {
        requireOwner(userId, accountId);
        return txns.findTop20ByAccountIdOrderByCreatedAtDesc(accountId);
    }

    private void requireOwner(UUID userId, UUID accountId) {
        Account a = accounts.findById(accountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found"));
        if (!a.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "This account does not belong to you");
        }
    }
}
