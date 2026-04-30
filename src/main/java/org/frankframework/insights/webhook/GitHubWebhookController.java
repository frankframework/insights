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

    @Value("${insights.webhook.secret:}")
    private String webhookSecret;

    public GitHubWebhookController(SystemDataInitializer systemDataInitializer, ObjectMapper objectMapper) {
        this.systemDataInitializer = systemDataInitializer;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/github")
    public ResponseEntity<String> handleGitHubWebhook(
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody byte[] body) {

        if (isWebhookSecretMissing()) {
            log.error("Webhook Error: insights.webhook.secret is empty in application properties.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server missing webhook secret configuration");
        }

        if (!isValidSignature(signature, body)) {
            log.warn("GitHub webhook rejected: invalid or missing X-Hub-Signature-256. Provided: {}", signature);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Signature");
        }

        log.debug("Received verified GitHub event: {}", event);

        if (isIgnoredEvent(event)) {
            return ResponseEntity.ok("Event ignored: " + event);
        }

        return handleReleaseEvent(body);
    }

    private ResponseEntity<String> handleReleaseEvent(byte[] body) {
        try {
            String action = parseAction(body);

            if (!ACTION_PUBLISHED.equals(action)) {
                log.debug("Ignored release action: {}", action);
                return ResponseEntity.ok("Action ignored: " + action);
            }
        } catch (Exception e) {
            log.error("Failed to process webhook JSON payload", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid JSON payload");
        }

        scheduleRefresh();

        return ResponseEntity.accepted().body("Refresh scheduled");
    }

	private void scheduleRefresh() {
		try {
			systemDataInitializer.triggerRefresh();
		} catch (Exception e) {
			log.error("Failed to schedule data refresh", e);
		}
	}

    private String parseAction(byte[] body) throws Exception {
        return objectMapper.readTree(body).path("action").asText();
    }

    private boolean isValidSignature(String signature, byte[] body) {
        if (signature == null || !signature.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }
        try {
            SecretKeySpec signingKey =
                    new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(signingKey);

            byte[] expectedHash = mac.doFinal(body);
            String actualHashHex = signature.substring(SIGNATURE_PREFIX.length());
            byte[] actualHash = HexFormat.of().parseHex(actualHashHex);

            return java.security.MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            log.error("Signature calculation failed", e);
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
