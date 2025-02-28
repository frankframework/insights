package org.frankframework.insights.service;


import java.util.Arrays;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Set;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.IssueDTO;
import org.frankframework.insights.dto.LabelDTO;
import org.frankframework.insights.mapper.IssueMapper;
import org.frankframework.insights.mapper.Mapper;
import org.frankframework.insights.models.Issue;
import org.frankframework.insights.models.Label;
import org.frankframework.insights.repository.IssueRepository;
import org.springframework.stereotype.Service;

@Service
public class IssueService {

    private final GitHubClient gitHubClient;

    private final IssueMapper issueMapper;

    private final IssueRepository issueRepository;

    public IssueService(GitHubClient gitHubClient, IssueMapper issueMapper, IssueRepository issueRepository) {
        this.gitHubClient = gitHubClient;
        this.issueMapper = issueMapper;
        this.issueRepository = issueRepository;
    }

    public void injectIssues() throws RuntimeException {
        if (!issueRepository.findAll().isEmpty()) {
            return;
        }

        try {
            JsonNode jsonIssues = gitHubClient.getIssues();

            Set<Issue> issues = issueMapper.jsonToIssues(jsonIssues);

            saveIssues(issues);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    private void saveIssues(Set<Issue> issues) {
        issueRepository.saveAll(issues);
    }
}
