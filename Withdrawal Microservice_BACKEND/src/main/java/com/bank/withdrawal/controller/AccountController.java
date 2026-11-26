package com.bank.withdrawal.controller;

import com.bank.withdrawal.dto.AccountResponse;
import com.bank.withdrawal.dto.BeneficiaryRequest;
import com.bank.withdrawal.repository.AccountRepository;
import com.bank.withdrawal.service.BeneficiaryService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    private final AccountRepository accounts;
    private final BeneficiaryService beneficiaries;

    public AccountController(AccountRepository accounts, BeneficiaryService beneficiaries) {
        this.accounts = accounts;
        this.beneficiaries = beneficiaries;
    }

    @GetMapping
    public List<AccountResponse> mine(Authentication auth) {
        return accounts.findByUserId(UUID.fromString(auth.getName())).stream().map(AccountResponse::from).toList();
    }

    @PostMapping("/{accountId}/beneficiary")
    public AccountResponse linkBeneficiary(@PathVariable UUID accountId,
                                           @Valid @RequestBody BeneficiaryRequest req, Authentication auth) {
        return beneficiaries.link(UUID.fromString(auth.getName()), accountId, req);
    }
}
