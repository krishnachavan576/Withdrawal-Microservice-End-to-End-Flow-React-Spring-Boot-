package com.bank.withdrawal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawalRequest(
        @NotNull UUID accountId,
        @NotNull @DecimalMin(value = "1.00", message = "Minimum withdrawal is ₹1")
        @Digits(integer = 12, fraction = 2, message = "Amount can have at most 2 decimal places") BigDecimal amount) {}
