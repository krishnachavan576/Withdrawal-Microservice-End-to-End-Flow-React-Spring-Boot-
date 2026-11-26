package com.bank.withdrawal.repository;

import com.bank.withdrawal.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByUserId(UUID userId);

    /** SELECT ... FOR UPDATE: two parallel withdrawals on one account are serialized by the DB. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") UUID id);

    /** Targeted update so we never overwrite a concurrently changed balance. */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
           update Account a set a.rzpContactId = :contactId, a.rzpFundAccountId = :fundId,
                  a.beneficiaryName = :name, a.beneficiaryIfsc = :ifsc, a.beneficiaryLast4 = :last4
           where a.id = :id
           """)
    int updateBeneficiary(@Param("id") UUID id, @Param("contactId") String contactId,
                          @Param("fundId") String fundId, @Param("name") String name,
                          @Param("ifsc") String ifsc, @Param("last4") String last4);
}
