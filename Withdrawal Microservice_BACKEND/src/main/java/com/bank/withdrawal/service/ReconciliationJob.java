package com.bank.withdrawal.service;

import com.bank.withdrawal.entity.Account;
import com.bank.withdrawal.entity.AccountTransaction;
import com.bank.withdrawal.entity.TxnStatus;
import com.bank.withdrawal.gateway.PayoutGateway;
import com.bank.withdrawal.gateway.PayoutGatewayException;
import com.bank.withdrawal.gateway.PayoutResult;
import com.bank.withdrawal.repository.AccountRepository;
import com.bank.withdrawal.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Safety net for lost webhooks, timeouts and crashes between "debited" and "payout created".
 * Row locks in TransactionService make it safe even if several instances run it.
 */
@Component
public class ReconciliationJob {
    private static final Logger log = LoggerFactory.getLogger(ReconciliationJob.class);

    private final TransactionRepository txns;
    private final AccountRepository accounts;
    private final PayoutGateway gateway;
    private final TransactionService txnService;
    private final long olderThanSeconds;

    public ReconciliationJob(TransactionRepository txns, AccountRepository accounts, PayoutGateway gateway,
                             TransactionService txnService,
                             @Value("${app.reconcile.pending-older-than-seconds}") long olderThanSeconds) {
        this.txns = txns;
        this.accounts = accounts;
        this.gateway = gateway;
        this.txnService = txnService;
        this.olderThanSeconds = olderThanSeconds;
    }

    @Scheduled(fixedDelayString = "${app.reconcile.interval-ms}")
    public void run() {
        Instant cutoff = Instant.now().minusSeconds(olderThanSeconds);
        for (AccountTransaction t : txns.findByStatusAndCreatedAtBefore(TxnStatus.PENDING, cutoff)) {
            try {
                PayoutResult r;
                if (t.getPayoutId() == null) {
                    // We crashed/timed out before learning the payout id. Retrying is safe: same idempotency key.
                    Account acc = accounts.findById(t.getAccountId()).orElseThrow();
                    r = gateway.createPayout(acc.getRzpFundAccountId(), t.getAmount(),
                            t.getId().toString(), t.getId().toString());
                    txnService.attachPayoutId(t.getId(), r.payoutId());
                } else {
                    r = gateway.fetchPayout(t.getPayoutId());
                }
                txnService.applyResult(t.getId(), r);
            } catch (PayoutGatewayException.Rejected e) {
                txnService.markFailedAndRefund(t.getId(), e.getMessage());
            } catch (Exception e) {
                log.warn("reconcile skipped txn={}: {}", t.getId(), e.getMessage());   // try again next run
            }
        }
    }
}
