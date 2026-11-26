package com.bank.withdrawal.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local demo gateway (razorpay.mock=true, the default). No network, no real money.
 * A payout stays "processing" for 8 seconds and then becomes "processed".
 * Trick for testing the failure path: withdraw an amount ending in .13 (for example 100.13) -> becomes "reversed".
 */
@Component
@ConditionalOnProperty(name = "razorpay.mock", havingValue = "true", matchIfMissing = true)
public class MockPayoutGateway implements PayoutGateway {

    private record Fake(BigDecimal amount, Instant createdAt) {}

    private final Map<String, Fake> payouts = new ConcurrentHashMap<>();

    @Override
    public String createContact(String name, String email, String phone, String referenceId) {
        return "cont_mock_" + shortId();
    }

    @Override
    public String createBankFundAccount(String contactId, String holderName, String ifsc, String accountNumber) {
        return "fa_mock_" + shortId();
    }

    @Override
    public PayoutResult createPayout(String fundAccountId, BigDecimal amountRupees, String referenceId, String idempotencyKey) {
        String id = "pout_mock_" + idempotencyKey.replace("-", "");   // deterministic => idempotent like the real API
        payouts.putIfAbsent(id, new Fake(amountRupees, Instant.now()));
        return new PayoutResult(id, "processing");
    }

    @Override
    public PayoutResult fetchPayout(String payoutId) {
        Fake f = payouts.get(payoutId);
        if (f == null) return new PayoutResult(payoutId, "processed");   // unknown after restart: assume done
        if (Duration.between(f.createdAt(), Instant.now()).getSeconds() < 8) return new PayoutResult(payoutId, "processing");
        boolean failCase = f.amount().remainder(BigDecimal.ONE).compareTo(new BigDecimal("0.13")) == 0;
        return new PayoutResult(payoutId, failCase ? "reversed" : "processed");
    }

    private static String shortId() { return UUID.randomUUID().toString().replace("-", "").substring(0, 12); }
}
