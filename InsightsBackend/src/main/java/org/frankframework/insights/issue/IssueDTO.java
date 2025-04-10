package org.frankframework.insights.issue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.frankframework.insights.github.GitHubEdgesDTO;
import org.frankframework.insights.github.GitHubPropertyState;
import org.frankframework.insights.label.LabelDTO;
import org.frankframework.insights.milestone.MilestoneDTO;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IssueDTO(
        String id,
        int number,
        String title,
        GitHubPropertyState state,
        String url,
        GitHubEdgesDTO<LabelDTO> labels,
        MilestoneDTO milestone,
		GitHubEdgesDTO<IssueDTO> subIssues
) {}
