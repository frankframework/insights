package org.frankframework.insights.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nvd.api")
@Getter
@Setter
public class NVDProperties {
    private String key;
    private int delay;
}
