package org.frankframework.webapp.common.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "cors.allowed")
@Getter
@Setter
public class CorsProperties {
    private List<String> origins;
}
