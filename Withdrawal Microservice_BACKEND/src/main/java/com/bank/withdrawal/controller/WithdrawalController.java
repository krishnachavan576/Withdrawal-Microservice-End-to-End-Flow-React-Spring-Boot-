package com.bank.withdrawal.controller;

import com.bank.withdrawal.dto.WithdrawalRequest;
import com.bank.withdrawal.dto.WithdrawalResponse;
import com.bank.withdrawal.exception.ApiException;
import com.bank.withdrawal.service.WithdrawalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/withdrawals")
public class WithdrawalController {
    private final WithdrawalService service;

    public WithdrawalController(WithdrawalService service) { this.service = service; }

    /** 201 = new withdrawal accepted (status is usually PENDING). 200 = duplicate request, original result returned. */
    @PostMapping
    public ResponseEntity<WithdrawalResponse> create(@Valid @RequestBody WithdrawalRequest req,
                                                     @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                     Authentication auth) {
        if (idempotencyKey.length() < 8 || idempotencyKey.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Idempotency-Key must be 8 to 100 characters");
        }
        var result = service.withdraw(UUID.fromString(auth.getName()), req.accountId(), req.amount(), idempotencyKey);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(WithdrawalResponse.from(result.txn()));
    }

    @GetMapping("/{id}")
    public WithdrawalResponse get(@PathVariable UUID id, Authentication auth) {
        return WithdrawalResponse.from(service.getForUser(UUID.fromString(auth.getName()), id));
    }

    @GetMapping
    public List<WithdrawalResponse> list(@RequestParam UUID accountId, Authentication auth) {
        return service.listForUser(UUID.fromString(auth.getName()), accountId).stream()
                .map(WithdrawalResponse::from).toList();
    }
}
