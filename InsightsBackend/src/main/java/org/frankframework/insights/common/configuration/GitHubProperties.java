package org.frankframework.insights.common.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "github")
@Getter
@Setter
public class GitHubProperties {
    private String url;
    private String secret;
    private String branchProtectionRegexes;

    public List<String> getBranchProtectionRegexes() {
        return Arrays.stream(branchProtectionRegexes.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }
}
