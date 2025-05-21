package org.frankframework.insights.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GitHubEdgesDTO<T> {
    @JsonProperty("edges")
    private List<GitHubNodeDTO<T>> edges;
}
