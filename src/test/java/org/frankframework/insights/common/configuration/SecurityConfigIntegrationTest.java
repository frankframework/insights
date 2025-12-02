package org.frankframework.insights.common.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.frankframework.insights.user.User;
import org.frankframework.insights.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local-seed")
public class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRegistry sessionRegistry;

    private OAuth2User oauth2User;

    @BeforeEach
    public void setUp() {
        User testUser = User.builder()
                .githubId(99999L)
                .username("testuser")
                .avatarUrl("https://github.com/avatars/testuser.png")
                .isFrankFrameworkMember(true)
                .build();
        userRepository.save(testUser);

        Map<String, Object> attributes = Map.of(
                "id", 99999L,
                "login", "testuser",
                "avatar_url", "https://github.com/avatars/testuser.png");

        oauth2User = new DefaultOAuth2User(
                java.util.Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")), attributes, "login");
    }

    @AfterEach
    public void tearDown() {
        userRepository.deleteAll();
        sessionRegistry.getAllPrincipals().forEach(principal -> sessionRegistry
                .getAllSessions(principal, false)
                .forEach(SessionInformation::expireNow));
    }

    @Test
    public void sessionIsCreated_whenAccessingPublicEndpoint_dueToCSRFToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/business-value/release/test"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(result.getResponse().getHeader("X-XSRF-TOKEN")).isNotNull();
    }

    @Test
    public void sessionIsCreated_whenAuthenticatedViaOAuth2() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/user")
                        .with(oauth2Login().oauth2User(oauth2User))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.isNew()).isTrue();
    }

    @Test
    public void sessionIdChanges_afterSuccessfulLogin_protectsAgainstSessionFixation() throws Exception {
        MvcResult result1 = mockMvc.perform(get("/api/auth/user")
                        .with(oauth2Login().oauth2User(oauth2User))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session1 = (MockHttpSession) result1.getRequest().getSession(false);
        assertThat(session1).isNotNull();
        String sessionId1 = session1.getId();

        MvcResult result2 = mockMvc.perform(get("/api/auth/user")
                        .with(oauth2Login().oauth2User(oauth2User))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session2 = (MockHttpSession) result2.getRequest().getSession(false);
        assertThat(session2).isNotNull();

        assertThat(session2.getId()).isNotEqualTo(sessionId1);
    }

    @Test
    public void maximumSessions_isEnforced_oldestSessionExpired_whenLimitExceeded() throws Exception {
        createAndRegisterAuthenticatedSession();
        createAndRegisterAuthenticatedSession();
        createAndRegisterAuthenticatedSession();

        var allSessions = sessionRegistry.getAllSessions(oauth2User, false);
        assertThat(allSessions).hasSize(3);
        assertThat(allSessions).noneMatch(SessionInformation::isExpired);

        MockHttpSession session4 = createAuthenticatedSession();

        allSessions = sessionRegistry.getAllSessions(oauth2User, false);
        if (allSessions.size() >= 3) {
            allSessions.getFirst().expireNow();
        }
        sessionRegistry.registerNewSession(session4.getId(), oauth2User);

        allSessions = sessionRegistry.getAllSessions(oauth2User, true);
        assertThat(allSessions).hasSize(4);
        assertThat(allSessions.stream().filter(SessionInformation::isExpired)).hasSize(1);
        assertThat(allSessions.stream().filter(s -> !s.isExpired())).hasSize(3);
    }

    @Test
    public void logout_invalidatesSession() throws Exception {
        MvcResult loginResult = mockMvc.perform(get("/api/auth/user")
                        .with(oauth2Login().oauth2User(oauth2User))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.isInvalid()).isFalse();

        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().is3xxRedirection());

        assertThat(session.isInvalid()).isTrue();
    }

    private MockHttpSession createAuthenticatedSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/user")
                        .with(oauth2Login().oauth2User(oauth2User))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private void createAndRegisterAuthenticatedSession() throws Exception {
        MockHttpSession session = createAuthenticatedSession();
        sessionRegistry.registerNewSession(session.getId(), oauth2User);
    }
}
