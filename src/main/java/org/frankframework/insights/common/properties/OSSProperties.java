package org.frankframework.insights.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oss.index")
@Getter
@Setter
public class OSSProperties {
    private String username;
    private String token;
}
