package com.bank.withdrawal.service;

import com.bank.withdrawal.entity.Account;
import com.bank.withdrawal.entity.AccountStatus;
import com.bank.withdrawal.entity.User;
import com.bank.withdrawal.repository.AccountRepository;
import com.bank.withdrawal.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** DEMO ONLY. Disable with app.seed-demo-data=false. */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final AccountRepository accounts;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, AccountRepository accounts, PasswordEncoder encoder) {
        this.users = users;
        this.accounts = accounts;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.findByUsername("demo").isPresent()) return;
        User u = new User();
        u.setUsername("demo");
        u.setPasswordHash(encoder.encode("demo123"));
        u.setFullName("Demo Customer");
        u.setEmail("demo@example.com");
        u.setPhone("9999999999");
        u = users.save(u);

        Account a = new Account();
        a.setAccountNumber("SB10002000300");
        a.setUserId(u.getId());
        a.setBalance(new BigDecimal("100000.00"));
        a.setStatus(AccountStatus.ACTIVE);
        a.setDailyLimit(new BigDecimal("100000.00"));
        accounts.save(a);
        log.info("Seeded demo user: demo / demo123");
    }
}
