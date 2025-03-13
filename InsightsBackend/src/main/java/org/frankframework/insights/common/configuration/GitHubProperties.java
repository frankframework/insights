package org.frankframework.insights.common.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "github.api")
@Getter
@Setter
public class GitHubProperties {
    private String url;
    private String secret;
}
