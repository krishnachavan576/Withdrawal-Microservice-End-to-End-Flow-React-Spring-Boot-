package com.bank.withdrawal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BeneficiaryRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Enter a valid IFSC, for example HDFC0001234") String ifsc,
        @NotBlank @Pattern(regexp = "^[0-9]{9,18}$", message = "Account number must be 9 to 18 digits") String accountNumber) {}
