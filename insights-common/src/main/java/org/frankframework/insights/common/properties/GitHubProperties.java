package org.frankframework.insights.common.properties;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github")
@Getter
@Setter
public class GitHubProperties {
    private GitHubGraphQLProperties graphql = new GitHubGraphQLProperties();
    private GitHubRestProperties rest = new GitHubRestProperties();

    @Getter
    @Setter
    public static class GitHubGraphQLProperties {
        private String url;
        private String secret;
        private String projectId;
        private List<String> branchProtectionRegexes;
        private List<String> includedLabels;

        public List<String> getIncludedLabels() {
            return includedLabels.stream().map(String::toUpperCase).toList();
        }
    }

    @Getter
    @Setter
    public static class GitHubRestProperties {
        private String url;
    }
}
