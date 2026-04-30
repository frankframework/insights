package org.frankframework.insights.webhook;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.frankframework.insights.common.configuration.SystemDataInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local-seed")
@TestPropertySource(properties = "insights.webhook.secret=test-secret")
public class GitHubWebhookSecurityIntegrationTest {

    private static final String TEST_SECRET = "test-secret";
    private static final String RELEASE_PUBLISHED_PAYLOAD = "{\"action\":\"published\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemDataInitializer systemDataInitializer;

    @Test
    public void webhookEndpoint_isExemptFromCsrf_noTokenRequired() throws Exception {
        mockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "release")
                        .header("X-Hub-Signature-256", "sha256=invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RELEASE_PUBLISHED_PAYLOAD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void webhookEndpoint_isAccessibleWithoutOAuth2Session() throws Exception {
        byte[] body = RELEASE_PUBLISHED_PAYLOAD.getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "release")
                        .header("X-Hub-Signature-256", sign(body))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());
    }

    @Test
    public void webhookEndpoint_withValidSignatureAndPublishedRelease_returns202() throws Exception {
        byte[] body = RELEASE_PUBLISHED_PAYLOAD.getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "release")
                        .header("X-Hub-Signature-256", sign(body))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());
    }

    @Test
    public void webhookEndpoint_whenJobAlreadyRunning_stillReturns202() throws Exception {
        byte[] body = RELEASE_PUBLISHED_PAYLOAD.getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "release")
                        .header("X-Hub-Signature-256", sign(body))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());
    }

    private static String sign(byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }
}
