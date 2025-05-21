package org.frankframework.insights.github;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GitHubEdgesDTO<T> {
    @JsonProperty("edges")
    private List<GitHubNodeDTO<T>> edges;
}
