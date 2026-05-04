package org.frankframework.insights.webhook;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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

@WebMvcTest(
        controllers = GitHubWebhookController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class})
@Import({TestSecurityConfig.class, MapperConfiguration.class})
@TestPropertySource(properties = "insights.webhook.secret=test-secret")
public class GitHubWebhookControllerTest {

    private static final String TEST_SECRET = "test-secret";
    private static final String RELEASE_PUBLISHED_PAYLOAD = "{\"action\":\"published\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemDataInitializer systemDataInitializer;

    @Test
    public void handleWebhook_whenReleasePublished_triggersRefreshAndReturns202() throws Exception {
        byte[] body = RELEASE_PUBLISHED_PAYLOAD.getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "release")
                        .header("X-Hub-Signature-256", sign(body))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());

        verify(systemDataInitializer).triggerRefresh();
    }

    @Test
    public void handleWebhook_whenJobAlreadyRunning_stillReturns202AndQueues() throws Exception {
        byte[] body = RELEASE_PUBLISHED_PAYLOAD.getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "release")
                        .header("X-Hub-Signature-256", sign(body))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());

        verify(systemDataInitializer).triggerRefresh();
    }

    @Test
    public void handleWebhook_whenRefreshFails_returns202Anyway() throws Exception {
        doThrow(new RuntimeException("GitHub unreachable"))
                .when(systemDataInitializer)
                .triggerRefresh();
        byte[] body = RELEASE_PUBLISHED_PAYLOAD.getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "release")
                        .header("X-Hub-Signature-256", sign(body))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());
    }

    @Test
    public void handleWebhook_whenSignatureInvalid_returns401() throws Exception {
        byte[] body = RELEASE_PUBLISHED_PAYLOAD.getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "release")
                        .header("X-Hub-Signature-256", "sha256=invalidsignature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verify(systemDataInitializer, never()).triggerRefresh();
    }

    @Test
    public void handleWebhook_whenSignatureMissing_returns401() throws Exception {
        byte[] body = RELEASE_PUBLISHED_PAYLOAD.getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verify(systemDataInitializer, never()).triggerRefresh();
    }

    @Test
    public void handleWebhook_whenEventIsNotRelease_returns200WithoutRefresh() throws Exception {
        byte[] body = "{\"action\":\"created\"}".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", sign(body))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(systemDataInitializer, never()).triggerRefresh();
    }

    @Test
    public void handleWebhook_whenReleaseActionIsNotPublished_returns200WithoutRefresh() throws Exception {
        byte[] body = "{\"action\":\"deleted\",\"release\":{\"tag_name\":\"v9.0\"}}".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "release")
                        .header("X-Hub-Signature-256", sign(body))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(systemDataInitializer, never()).triggerRefresh();
    }

    private static String sign(byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }
}
