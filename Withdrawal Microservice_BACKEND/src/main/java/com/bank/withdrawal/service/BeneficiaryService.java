package com.bank.withdrawal.service;

import com.bank.withdrawal.dto.AccountResponse;
import com.bank.withdrawal.dto.BeneficiaryRequest;
import com.bank.withdrawal.entity.Account;
import com.bank.withdrawal.entity.User;
import com.bank.withdrawal.exception.ApiException;
import com.bank.withdrawal.gateway.PayoutGateway;
import com.bank.withdrawal.gateway.PayoutGatewayException;
import com.bank.withdrawal.repository.AccountRepository;
import com.bank.withdrawal.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Razorpay step 1 + 2: create Contact and Fund Account, then remember only the ids (not the full account number). */
@Service
public class BeneficiaryService {
    private final AccountRepository accounts;
    private final UserRepository users;
    private final PayoutGateway gateway;

    public BeneficiaryService(AccountRepository accounts, UserRepository users, PayoutGateway gateway) {
        this.accounts = accounts;
        this.users = users;
        this.gateway = gateway;
    }

    public AccountResponse link(UUID userId, UUID accountId, BeneficiaryRequest req) {
        Account acc = accounts.findById(accountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found"));
        if (!acc.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "This account does not belong to you");
        }
        User user = users.findById(userId).orElseThrow();
        try {
            String contactId = acc.getRzpContactId() != null ? acc.getRzpContactId()
                    : gateway.createContact(user.getFullName(), user.getEmail(), user.getPhone(), acc.getId().toString());
            String fundId = gateway.createBankFundAccount(contactId, req.name(), req.ifsc(), req.accountNumber());
            String last4 = req.accountNumber().substring(req.accountNumber().length() - 4);
            accounts.updateBeneficiary(acc.getId(), contactId, fundId, req.name(), req.ifsc(), last4);
        } catch (PayoutGatewayException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "BENEFICIARY_FAILED",
                    "Could not verify this bank account. Check the details and try again.");
        }
        return AccountResponse.from(accounts.findById(accountId).orElseThrow());
    }
}
