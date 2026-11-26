package com.bank.withdrawal.dto;

import com.bank.withdrawal.entity.Account;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(UUID id, String maskedAccountNumber, BigDecimal balance, String status,
                              BigDecimal dailyLimit, boolean hasBeneficiary, String beneficiaryName,
                              String beneficiaryIfsc, String beneficiaryLast4) {
    public static AccountResponse from(Account a) {
        String n = a.getAccountNumber();
        String masked = "XXXX" + n.substring(Math.max(0, n.length() - 4));
        return new AccountResponse(a.getId(), masked, a.getBalance(), a.getStatus().name(), a.getDailyLimit(),
                a.getRzpFundAccountId() != null, a.getBeneficiaryName(), a.getBeneficiaryIfsc(), a.getBeneficiaryLast4());
    }
}
