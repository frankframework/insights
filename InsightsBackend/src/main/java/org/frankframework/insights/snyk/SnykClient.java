package org.frankframework.insights.snyk;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.client.rest.RestClient;
import org.frankframework.insights.common.configuration.properties.SnykProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SnykClient extends RestClient {
    private final String organisationId;
    private final ObjectMapper objectMapper;

    /**
     * Constructor that initializes the SnykClient with configuration properties.
     * @param snykProperties The properties containing Snyk API configuration such as URL, token, organization ID, and version.
     */
    public SnykClient(SnykProperties snykProperties, ObjectMapper objectMapper) {
        super(snykProperties.getUrl(), builder -> {
            if (snykProperties.getToken() != null && !snykProperties.getToken().isEmpty()) {
                builder.defaultHeader(HttpHeaders.AUTHORIZATION, "token " + snykProperties.getToken());
            }
        });
        this.organisationId = snykProperties.getOrgId();
        this.objectMapper = objectMapper;
    }

    // todo add Snyk API calls for service here
}
