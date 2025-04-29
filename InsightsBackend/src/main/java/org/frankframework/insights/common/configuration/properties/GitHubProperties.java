package org.frankframework.insights.common.configuration.properties;

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
    private List<String> branchProtectionRegexes;
    private Boolean fetch;
}
