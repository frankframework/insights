package org.frankframework.webapp.common.configuration.properties;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github")
@Getter
@Setter
public class GitHubProperties {
    private String url;
    private String secret;
    private String projectId;
    private List<String> branchProtectionRegexes;
    private List<String> includedLabels;
    private Boolean fetch;

    public List<String> getIncludedLabels() {
        return includedLabels.stream().map(String::toUpperCase).toList();
    }
}
