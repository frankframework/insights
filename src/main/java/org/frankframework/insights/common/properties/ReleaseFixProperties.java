package org.frankframework.insights.common.properties;

import java.time.OffsetDateTime;
import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "release-fixes")
@Getter
@Setter
public class ReleaseFixProperties {
    private HashMap<String, OffsetDateTime> dateOverrides = new HashMap<>();
}
