package org.frankframework.insights.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import java.util.Set;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.exceptions.GitHubDataInjectionException;
import org.frankframework.insights.mapper.IssueMapper;
import org.frankframework.insights.models.Issue;
import org.frankframework.insights.repository.IssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IssueService {

    private final GitHubClient gitHubClient;

    private final IssueMapper issueMapper;

    private final IssueRepository issueRepository;

    @Autowired
    public IssueService(GitHubClient gitHubClient, IssueMapper issueMapper, IssueRepository issueRepository) {
        this.gitHubClient = gitHubClient;
        this.issueMapper = issueMapper;
        this.issueRepository = issueRepository;
    }

    public void injectIssues() {
        if (!issueRepository.findAll().isEmpty()) {
            return;
        }

        try {
            JsonNode jsonIssues = gitHubClient.getIssues();

            Set<Issue> issues = issueMapper.jsonToIssues(jsonIssues);

            saveIssues(issues);
        } catch (RuntimeException e) {
            throw new GitHubDataInjectionException();
        }
    }

    @Transactional
    private void saveIssues(Set<Issue> issues) {
        issueRepository.saveAll(issues);
    }
}
