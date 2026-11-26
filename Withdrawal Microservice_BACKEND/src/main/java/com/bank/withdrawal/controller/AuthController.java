package com.bank.withdrawal.controller;

import com.bank.withdrawal.dto.LoginRequest;
import com.bank.withdrawal.dto.LoginResponse;
import com.bank.withdrawal.entity.User;
import com.bank.withdrawal.exception.ApiException;
import com.bank.withdrawal.repository.UserRepository;
import com.bank.withdrawal.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        User u = users.findByUsername(req.username())
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Wrong username or password"));
        return new LoginResponse(jwt.generate(u.getId(), u.getUsername()), u.getFullName());
    }
}
