package com.bank.withdrawal.gateway;

public class PayoutGatewayException extends RuntimeException {
    public PayoutGatewayException(String message, Throwable cause) { super(message, cause); }

    /** Razorpay clearly refused (4xx). Nothing was paid out, so it is safe to refund. */
    public static class Rejected extends PayoutGatewayException {
        public Rejected(String message, Throwable cause) { super(message, cause); }
    }

    /** Timeout / 5xx / 429. The payout MAY have been created: never refund on this, reconcile instead. */
    public static class Unknown extends PayoutGatewayException {
        public Unknown(String message, Throwable cause) { super(message, cause); }
    }
}
