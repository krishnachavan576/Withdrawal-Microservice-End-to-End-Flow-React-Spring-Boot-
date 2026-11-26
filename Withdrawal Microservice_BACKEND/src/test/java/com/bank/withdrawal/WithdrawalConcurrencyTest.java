package com.bank.withdrawal;

import com.bank.withdrawal.entity.Account;
import com.bank.withdrawal.entity.AccountStatus;
import com.bank.withdrawal.entity.User;
import com.bank.withdrawal.exception.ApiException;
import com.bank.withdrawal.repository.AccountRepository;
import com.bank.withdrawal.repository.UserRepository;
import com.bank.withdrawal.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Needs Docker. 50 parallel withdrawals of ₹100 against a ₹1000 balance: exactly 10 may succeed. */
@SpringBootTest(properties = "app.seed-demo-data=false")
@Testcontainers
class WithdrawalConcurrencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired UserRepository users;
    @Autowired AccountRepository accounts;
    @Autowired TransactionService txnService;

    @Test
    void parallelWithdrawalsNeverOverdraw() throws Exception {
        User u = new User();
        u.setUsername("t-" + UUID.randomUUID());
        u.setPasswordHash("x");
        u.setFullName("T");
        u.setEmail("t@t.com");
        u.setPhone("9999999999");
        u = users.save(u);

        Account a = new Account();
        a.setAccountNumber("ACC" + System.nanoTime());
        a.setUserId(u.getId());
        a.setBalance(new BigDecimal("1000.00"));
        a.setStatus(AccountStatus.ACTIVE);
        a.setDailyLimit(new BigDecimal("100000.00"));
        a.setRzpFundAccountId("fa_test");
        a = accounts.save(a);

        final UUID userId = u.getId();
        final UUID accountId = a.getId();
        ExecutorService pool = Executors.newFixedThreadPool(20);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            results.add(pool.submit(() -> {
                try {
                    txnService.debitAndCreatePending(userId, accountId, new BigDecimal("100.00"), UUID.randomUUID().toString());
                    return true;
                } catch (ApiException e) {
                    return false;   // INSUFFICIENT_FUNDS
                }
            }));
        }
        int ok = 0;
        for (Future<Boolean> f : results) if (f.get(30, TimeUnit.SECONDS)) ok++;
        pool.shutdown();

        assertEquals(10, ok);
        assertEquals(0, accounts.findById(accountId).orElseThrow().getBalance().compareTo(BigDecimal.ZERO));
    }
}
