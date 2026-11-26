package com.bank.withdrawal.gateway;

import java.util.Set;

/** status values follow Razorpay: queued, pending, processing, processed, cancelled, rejected, failed, reversed. */
public record PayoutResult(String payoutId, String status) {
    private static final Set<String> FAILED = Set.of("failed", "rejected", "cancelled", "reversed");

    public boolean isProcessed() { return "processed".equals(status); }
    public boolean isFailed() { return status != null && FAILED.contains(status); }
}
