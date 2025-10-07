package org.frankframework.insights.common.configuration.properties;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cors.allowed")
@Getter
@Setter
public class CorsProperties {
    private List<String> origins;
}
