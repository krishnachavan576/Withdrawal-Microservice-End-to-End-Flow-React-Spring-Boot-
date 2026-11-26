package com.bank.withdrawal.controller;

import com.bank.withdrawal.config.RazorpayProperties;
import com.bank.withdrawal.entity.AccountTransaction;
import com.bank.withdrawal.repository.TransactionRepository;
import com.bank.withdrawal.service.TransactionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/** Razorpay calls this when a payout changes state. Configure URL + secret in the Razorpay dashboard. */
@RestController
public class RazorpayWebhookController {
    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private final RazorpayProperties props;
    private final TransactionService txnService;
    private final TransactionRepository txns;
    private final ObjectMapper mapper;

    public RazorpayWebhookController(RazorpayProperties props, TransactionService txnService,
                                     TransactionRepository txns, ObjectMapper mapper) {
        this.props = props;
        this.txnService = txnService;
        this.txns = txns;
        this.mapper = mapper;
    }

    @PostMapping("/webhooks/razorpay")
    public ResponseEntity<Void> handle(@RequestBody String rawBody,
                                       @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) throws Exception {
        // Verify against the RAW body, exactly as received.
        if (signature == null || !validSignature(rawBody, signature)) {
            log.warn("webhook rejected: bad signature");
            return ResponseEntity.status(401).build();
        }

        JsonNode root = mapper.readTree(rawBody);
        String event = root.path("event").asText();
        JsonNode payout = root.path("payload").path("payout").path("entity");
        String payoutId = payout.path("id").asText(null);
        String referenceId = payout.path("reference_id").asText(null);   // our txn id; works even if webhook beats attachPayoutId()

        Optional<AccountTransaction> found = Optional.empty();
        if (referenceId != null) {
            try { found = txns.findById(UUID.fromString(referenceId)); } catch (IllegalArgumentException ignored) { }
        }
        if (found.isEmpty() && payoutId != null) found = txns.findByPayoutId(payoutId);

        if (found.isEmpty()) {
            log.warn("webhook for unknown payout event={} payout={}", event, payoutId);
            return ResponseEntity.ok().build();   // 200 so Razorpay stops retrying
        }

        UUID txnId = found.get().getId();
        switch (event) {
            case "payout.processed" -> txnService.markSuccess(txnId);
            case "payout.failed", "payout.rejected", "payout.reversed" ->
                    txnService.markFailedAndRefund(txnId, event + ": " + payout.path("failure_reason").asText(""));
            default -> { /* queued / initiated / pending: nothing to do */ }
        }
        return ResponseEntity.ok().build();
    }

    private boolean validSignature(String body, String signature) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(props.webhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expected = HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }
}
