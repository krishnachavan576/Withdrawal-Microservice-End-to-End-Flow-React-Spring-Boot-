package com.bank.withdrawal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter @Setter @NoArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String accountNumber;
    private UUID userId;
    private BigDecimal balance;
    @Enumerated(EnumType.STRING)
    private AccountStatus status;
    private BigDecimal dailyLimit;

    // Razorpay beneficiary (where withdrawals are paid out). Full bank account number is NOT stored.
    private String rzpContactId;
    private String rzpFundAccountId;
    private String beneficiaryName;
    private String beneficiaryIfsc;
    private String beneficiaryLast4;

    @Version
    private Long version;
}
