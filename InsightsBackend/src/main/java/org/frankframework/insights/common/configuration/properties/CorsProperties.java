package org.frankframework.insights.common.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cors.allowed")
@Getter
@Setter
public class CorsProperties {
    private String[] origins;
}
