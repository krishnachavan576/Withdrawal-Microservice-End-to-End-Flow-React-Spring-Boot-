package com.bank.withdrawal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter @Setter @NoArgsConstructor
public class AccountTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID accountId;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    @Enumerated(EnumType.STRING)
    private TxnStatus status;
    private String idempotencyKey;
    private String payoutId;
    private String failureReason;
    private boolean refunded;
    private Instant createdAt;
    private Instant updatedAt;
}
