package org.frankframework.insights.common.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fetch")
@Getter
@Setter
public class FetchProperties {
    private Boolean enabled;
}
