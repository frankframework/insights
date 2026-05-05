package org.frankframework.insights.webhook;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.frankframework.insights.common.configuration.MapperConfiguration;
import org.frankframework.insights.common.configuration.SystemDataInitializer;
import org.frankframework.insights.common.configuration.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that the controller trims whitespace from the webhook secret.
 */
@WebMvcTest(
        controllers = GitHubWebhookController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class})
@Import({TestSecurityConfig.class, MapperConfiguration.class})
@TestPropertySource(properties = "insights.webhook.secret=test-secret ") // trailing space simulates env-var whitespace
public class GitHubWebhookSecretTrimmingTest {

    private static final String CLEAN_SECRET = "test-secret";
    private static final String RELEASE_PUBLISHED_PAYLOAD = "{\"action\":\"published\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemDataInitializer systemDataInitializer;

    @Test
    public void handleWebhook_whenEnvVarSecretHasTrailingSpace_signatureStillValidates() throws Exception {
        byte[] body = RELEASE_PUBLISHED_PAYLOAD.getBytes(StandardCharsets.UTF_8);

        String signatureFromGitHub = signWith(CLEAN_SECRET, body);

        mockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "release")
                        .header("X-Hub-Signature-256", signatureFromGitHub)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());

        verify(systemDataInitializer).triggerRefresh();
    }

    @Test
    public void handleWebhook_whenEnvVarSecretHasTrailingSpace_wrongSignatureFails() throws Exception {
        byte[] body = RELEASE_PUBLISHED_PAYLOAD.getBytes(StandardCharsets.UTF_8);

        String wrongSignature = signWith(CLEAN_SECRET + " ", body);

        mockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "release")
                        .header("X-Hub-Signature-256", wrongSignature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    private static String signWith(String secret, byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }
}
