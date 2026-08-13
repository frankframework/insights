package org.frankframework.insights.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.configuration.SystemDataInitializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/webhooks")
public class GitHubWebhookController {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final String EVENT_RELEASE = "release";
    private static final String ACTION_PUBLISHED = "published";

    private final SystemDataInitializer systemDataInitializer;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public GitHubWebhookController(
            SystemDataInitializer systemDataInitializer,
            ObjectMapper objectMapper,
            @Value("${insights.webhook.secret:}") String webhookSecret) {
        this.systemDataInitializer = systemDataInitializer;
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret.trim();
    }

    @PostMapping("/github")
    public ResponseEntity<String> handleGitHubWebhook(
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody byte[] body) {

        log.info(
                "Received GitHub webhook: event='{}', body={} bytes, signature={}",
                event,
                body.length,
                signature != null ? signature : "<missing>");

        if (isWebhookSecretMissing()) {
            log.error("Webhook rejected: insights.webhook.secret is not configured on the server");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server missing webhook secret configuration");
        }

        if (!isValidSignature(signature, body)) {
            log.warn(
                    "Webhook rejected: signature mismatch or missing header."
                            + " Received='{}', body={} bytes, configured secret length={}",
                    signature,
                    body.length,
                    webhookSecret.length());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Signature");
        }

        log.info("Webhook signature verified. Processing event='{}'", event);

        if (isIgnoredEvent(event)) {
            log.debug("Ignored non-release event: '{}'", event);
            return ResponseEntity.ok("Event ignored: " + event);
        }

        return handleReleaseEvent(body);
    }

    private ResponseEntity<String> handleReleaseEvent(byte[] body) {
        try {
            String action = parseAction(body);
            log.info("Release event received with action='{}'", action);

            if (!ACTION_PUBLISHED.equals(action)) {
                log.debug("Ignored release action='{}' (only '{}' triggers a refresh)", action, ACTION_PUBLISHED);
                return ResponseEntity.ok("Action ignored: " + action);
            }
        } catch (Exception e) {
            log.error("Failed to parse webhook JSON payload", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid JSON payload");
        }

        log.info("Release published — scheduling data refresh");
        scheduleRefresh();

        return ResponseEntity.accepted().body("Refresh scheduled");
    }

    private void scheduleRefresh() {
        try {
            systemDataInitializer.triggerRefresh();
            log.info("Data refresh triggered successfully");
        } catch (Exception e) {
            log.error("Failed to trigger data refresh", e);
        }
    }

    private String parseAction(byte[] body) throws Exception {
        return objectMapper.readTree(body).path("action").asText();
    }

    private boolean isValidSignature(String signature, byte[] body) {
        if (signature == null) {
            log.warn("Signature validation failed: X-Hub-Signature-256 header is missing");
            return false;
        }
        if (!signature.startsWith(SIGNATURE_PREFIX)) {
            log.warn(
                    "Signature validation failed: header does not start with '{}', got '{}'",
                    SIGNATURE_PREFIX,
                    signature);
            return false;
        }
        try {
            SecretKeySpec signingKey =
                    new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(signingKey);

            byte[] expectedHash = mac.doFinal(body);
            String expectedHashHex = HexFormat.of().formatHex(expectedHash);
            String actualHashHex = signature.substring(SIGNATURE_PREFIX.length());
            byte[] actualHash = HexFormat.of().parseHex(actualHashHex);

            boolean valid = java.security.MessageDigest.isEqual(expectedHash, actualHash);
            if (!valid) {
                log.warn(
                        "Signature validation failed: HMAC mismatch."
                                + " Expected sha256={}, received sha256={}, body={} bytes, secret length={}",
                        expectedHashHex,
                        actualHashHex,
                        body.length,
                        webhookSecret.length());
            }

            return valid;
        } catch (Exception e) {
            log.error("Signature validation failed: exception during HMAC computation", e);
            return false;
        }
    }

    private boolean isWebhookSecretMissing() {
        return !StringUtils.hasText(webhookSecret);
    }

    private boolean isIgnoredEvent(String event) {
        return !EVENT_RELEASE.equals(event);
    }
}
