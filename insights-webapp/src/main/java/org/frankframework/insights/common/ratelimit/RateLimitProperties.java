package org.frankframework.insights.common.ratelimit;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Request paths the rate limiter acts on, configurable through {@code insights.rate-limit.*}.
 * <p>
 * A request is rate limited when its URI starts with one of the {@code protected-paths} and with
 * none of the {@code exempt-paths}; the exempt paths carve out sub-paths of a protected path that
 * must stay reachable regardless of a user's rate limit state.
 */
@ConfigurationProperties(prefix = "insights.rate-limit")
@Getter
@Setter
public class RateLimitProperties {
    private List<String> protectedPaths = List.of();
    private List<String> exemptPaths = List.of();
}
