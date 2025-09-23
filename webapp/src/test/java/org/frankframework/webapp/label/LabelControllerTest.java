package org.frankframework.webapp.label;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.Set;
import org.frankframework.webapp.common.mapper.MappingException;
import org.frankframework.webapp.release.ReleaseNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LabelController.class)
public class LabelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LabelService labelService;

    @TestConfiguration
    public static class MockConfig {
        @Bean
        public LabelService labelService() {
            return mock(LabelService.class);
        }
    }

    @BeforeEach
    public void resetMocks() {
        reset(labelService);
    }

    @Test
    public void getHighlightsByReleaseId_returnsSet() throws Exception {
        LabelResponse label1 = new LabelResponse("id1", "label1", "description1", "color1");
        LabelResponse label2 = new LabelResponse("id2", "label2", "description2", "color2");
        Set<LabelResponse> highlights = Set.of(label1, label2);

        when(labelService.getHighlightsByReleaseId("rel1")).thenReturn(highlights);

        mockMvc.perform(get("/api/labels/release/rel1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    public void getHighlightsByReleaseId_returnsEmptySet() throws Exception {
        when(labelService.getHighlightsByReleaseId("rel2")).thenReturn(Collections.emptySet());

        mockMvc.perform(get("/api/labels/release/rel2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void getHighlightsByReleaseId_serviceReturnsNull_treatedAsEmptySet() throws Exception {
        when(labelService.getHighlightsByReleaseId("rel3")).thenReturn(null);

        mockMvc.perform(get("/api/labels/release/rel3"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void getHighlightsByReleaseId_serviceThrowsReleaseNotFoundException_returns500() throws Exception {
        when(labelService.getHighlightsByReleaseId("notfound"))
                .thenThrow(new ReleaseNotFoundException("Not found", null));

        mockMvc.perform(get("/api/labels/release/notfound")).andExpect(status().isNotFound());
    }

    @Test
    public void getHighlightsByReleaseId_serviceThrowsMa1ppingException_returns500() throws Exception {
        when(labelService.getHighlightsByReleaseId("badmap")).thenThrow(new MappingException("Mapping failed", null));

        mockMvc.perform(get("/api/labels/release/badmap")).andExpect(status().isBadRequest());
    }
}
