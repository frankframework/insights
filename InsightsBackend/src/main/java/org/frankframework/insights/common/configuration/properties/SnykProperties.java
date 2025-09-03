package org.frankframework.insights.common.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "snyk.api")
@Getter
@Setter
public class SnykProperties {
    private String url;
    private String token;
    private String orgId;
    private String version;
}
