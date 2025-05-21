package org.frankframework.insights.release;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReleaseDTO {
    public String id;

    public String tagName;

    public String name;

    public OffsetDateTime publishedAt;
}
