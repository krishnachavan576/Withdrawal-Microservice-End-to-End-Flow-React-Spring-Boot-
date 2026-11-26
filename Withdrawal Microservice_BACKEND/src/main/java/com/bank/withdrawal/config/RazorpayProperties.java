package com.bank.withdrawal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "razorpay")
public record RazorpayProperties(boolean mock, String keyId, String keySecret,
                                 String accountNumber, String webhookSecret) {}
