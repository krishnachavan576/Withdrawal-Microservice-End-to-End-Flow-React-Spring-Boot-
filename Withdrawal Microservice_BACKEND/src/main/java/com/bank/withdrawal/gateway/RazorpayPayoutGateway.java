package com.bank.withdrawal.gateway;

import com.bank.withdrawal.config.RazorpayProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** Real RazorpayX Payouts API. Active when razorpay.mock=false. */
@Component
@ConditionalOnProperty(name = "razorpay.mock", havingValue = "false")
public class RazorpayPayoutGateway implements PayoutGateway {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {};

    private final RestClient client;
    private final String accountNumber;

    @Autowired
    public RazorpayPayoutGateway(RazorpayProperties p) {
        if (isBlank(p.keyId()) || isBlank(p.keySecret()) || isBlank(p.accountNumber())) {
            throw new IllegalStateException("razorpay.key-id, key-secret and account-number are required when razorpay.mock=false");
        }
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5_000);
        f.setReadTimeout(15_000);
        String basic = Base64.getEncoder().encodeToString((p.keyId() + ":" + p.keySecret()).getBytes(StandardCharsets.UTF_8));
        this.client = RestClient.builder()
                .requestFactory(f)
                .baseUrl("https://api.razorpay.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                .build();
        this.accountNumber = p.accountNumber();
    }

    @Override
    public String createContact(String name, String email, String phone, String referenceId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("contact", phone);
        body.put("type", "customer");
        body.put("reference_id", referenceId);
        return (String) send(HttpMethod.POST, "/contacts", body, null).get("id");
    }

    @Override
    public String createBankFundAccount(String contactId, String holderName, String ifsc, String accNo) {
        Map<String, Object> body = Map.of(
                "contact_id", contactId,
                "account_type", "bank_account",
                "bank_account", Map.of("name", holderName, "ifsc", ifsc, "account_number", accNo));
        return (String) send(HttpMethod.POST, "/fund_accounts", body, null).get("id");
    }

    @Override
    public PayoutResult createPayout(String fundAccountId, BigDecimal amountRupees, String referenceId, String idempotencyKey) {
        long paise = amountRupees.movePointRight(2).longValueExact();   // Razorpay amounts are in paise
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("account_number", accountNumber);                      // your RazorpayX account
        body.put("fund_account_id", fundAccountId);
        body.put("amount", paise);
        body.put("currency", "INR");
        body.put("mode", "IMPS");
        body.put("purpose", "payout");
        body.put("queue_if_low_balance", false);
        body.put("reference_id", referenceId);                          // = our transaction id, also comes back in webhooks
        body.put("narration", "Withdrawal");
        Map<String, Object> res = send(HttpMethod.POST, "/payouts", body, idempotencyKey);
        return new PayoutResult((String) res.get("id"), (String) res.get("status"));
    }

    @Override
    public PayoutResult fetchPayout(String payoutId) {
        Map<String, Object> res = send(HttpMethod.GET, "/payouts/" + payoutId, null, null);
        return new PayoutResult((String) res.get("id"), (String) res.get("status"));
    }

    private Map<String, Object> send(HttpMethod method, String path, Object body, String idempotencyKey) {
        try {
            RestClient.RequestBodySpec spec = client.method(method).uri(path).contentType(MediaType.APPLICATION_JSON);
            if (idempotencyKey != null) spec = spec.header("X-Payout-Idempotency", idempotencyKey);
            if (body != null) spec = spec.body(body);
            return spec.retrieve().body(MAP);
        } catch (RestClientResponseException e) {
            int s = e.getStatusCode().value();
            String detail = truncate(e.getResponseBodyAsString());
            if (s >= 400 && s < 500 && s != 429 && s != 408) {
                throw new PayoutGatewayException.Rejected("Razorpay rejected request (" + s + "): " + detail, e);
            }
            throw new PayoutGatewayException.Unknown("Razorpay error " + s, e);
        } catch (ResourceAccessException e) {
            throw new PayoutGatewayException.Unknown("Razorpay unreachable or timed out", e);
        }
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String truncate(String s) { return s == null ? "" : s.substring(0, Math.min(300, s.length())); }
}
