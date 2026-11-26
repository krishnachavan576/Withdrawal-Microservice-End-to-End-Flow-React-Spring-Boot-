package com.bank.withdrawal.repository;

import com.bank.withdrawal.entity.AccountTransaction;
import com.bank.withdrawal.entity.TxnStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<AccountTransaction, UUID> {

    Optional<AccountTransaction> findByAccountIdAndIdempotencyKey(UUID accountId, String idempotencyKey);

    Optional<AccountTransaction> findByPayoutId(String payoutId);

    List<AccountTransaction> findTop20ByAccountIdOrderByCreatedAtDesc(UUID accountId);

    List<AccountTransaction> findByStatusAndCreatedAtBefore(TxnStatus status, Instant before);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AccountTransaction t where t.id = :id")
    Optional<AccountTransaction> findByIdForUpdate(@Param("id") UUID id);

    /** Null when there are no rows. */
    @Query("""
           select sum(t.amount) from AccountTransaction t
           where t.accountId = :accountId and t.status in :statuses and t.createdAt >= :since
           """)
    BigDecimal sumSince(@Param("accountId") UUID accountId,
                        @Param("statuses") Collection<TxnStatus> statuses,
                        @Param("since") Instant since);
}
