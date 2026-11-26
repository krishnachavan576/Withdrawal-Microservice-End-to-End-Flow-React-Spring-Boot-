package com.bank.withdrawal.gateway;

import java.math.BigDecimal;

public interface PayoutGateway {
    String createContact(String name, String email, String phone, String referenceId);

    String createBankFundAccount(String contactId, String holderName, String ifsc, String accountNumber);

    /** idempotencyKey must be the same on every retry of the same withdrawal. */
    PayoutResult createPayout(String fundAccountId, BigDecimal amountRupees, String referenceId, String idempotencyKey);

    PayoutResult fetchPayout(String payoutId);
}
