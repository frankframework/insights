package org.frankframework.insights.issueprojects;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.properties.GitHubProperties;
import org.frankframework.insights.github.graphql.GitHubGraphQLClient;
import org.frankframework.insights.github.graphql.GitHubSingleSelectDTO;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IssueProjectItemsService {
    private final IssuePriorityRepository issuePriorityRepository;
    private final IssueStateRepository issueStateRepository;
    private final GitHubGraphQLClient gitHubGraphQLClient;
    private final Mapper mapper;
    private final String projectId;
    private static final String PRIORITY_FIELD_NAME = "Priority";
    private static final String STATE_FIELD_NAME = "Status";

    public IssueProjectItemsService(
            IssuePriorityRepository issuePriorityRepository,
            IssueStateRepository issueStateRepository,
            GitHubGraphQLClient gitHubGraphQLClient,
            Mapper mapper,
            GitHubProperties gitHubProperties) {
        this.issuePriorityRepository = issuePriorityRepository;
        this.issueStateRepository = issueStateRepository;
        this.gitHubGraphQLClient = gitHubGraphQLClient;
        this.mapper = mapper;
        this.projectId = gitHubProperties.getGraphql().getProjectId();
    }

    public void injectIssueProjectItems() throws IssueProjectItemInjectionException {
        if (issuePriorityRepository.count() != 0 && issueStateRepository.count() != 0) {
            log.info("Issue project items already found in the database");
            return;
        }

        try {
            log.info("Start injecting GitHub issue project items");
            Set<GitHubSingleSelectDTO.SingleSelectObject> projectItems;

            projectItems = gitHubGraphQLClient.getIssueProjectItems(projectId);

            if (issuePriorityRepository.count() == 0) {
                Set<IssuePriority> issuePriorities = fetchAndMapGithubPriorityOptions(projectItems);
                saveIssuePriorities(issuePriorities);
            } else {
                log.info("Issue priorities already found in the database");
            }

            if (issueStateRepository.count() == 0) {
                Set<IssueState> issueStates = fetchAndMapGithubStateOptions(projectItems);
                saveIssueStates(issueStates);
            } else {
                log.info("Issue states already found in the database");
            }
        } catch (Exception e) {
            throw new IssueProjectItemInjectionException("Error while injecting GitHub issue project items", e);
        }
    }

    private Set<IssuePriority> fetchAndMapGithubPriorityOptions(
            Set<GitHubSingleSelectDTO.SingleSelectObject> singleSelectObjects) {
        return singleSelectObjects.stream()
                .filter(dto -> dto.name() != null)
                .filter(dto -> PRIORITY_FIELD_NAME.equalsIgnoreCase(dto.name()))
                .filter(dto -> dto.options() != null && !dto.options().isEmpty())
                .findFirst()
                .map(this::mapPriorityOptionsToEntities)
                .orElseGet(Set::of);
    }

    private Set<IssueState> fetchAndMapGithubStateOptions(
            Set<GitHubSingleSelectDTO.SingleSelectObject> singleSelectObjects) {
        return singleSelectObjects.stream()
                .filter(dto -> dto.name() != null)
                .filter(dto -> STATE_FIELD_NAME.equalsIgnoreCase(dto.name()))
                .filter(dto -> dto.options() != null && !dto.options().isEmpty())
                .findFirst()
                .map(this::mapStateOptionsToEntities)
                .orElseGet(Set::of);
    }

    private Set<IssuePriority> mapPriorityOptionsToEntities(GitHubSingleSelectDTO.SingleSelectObject priorityDTO) {
        if (priorityDTO.options() == null) return Set.of();
        return priorityDTO.options().stream()
                .map(option -> mapper.toEntity(option, IssuePriority.class))
                .collect(Collectors.toSet());
    }

    private Set<IssueState> mapStateOptionsToEntities(GitHubSingleSelectDTO.SingleSelectObject stateDTO) {
        if (stateDTO.options() == null) return Set.of();
        return stateDTO.options().stream()
                .map(option -> mapper.toEntity(option, IssueState.class))
                .collect(Collectors.toSet());
    }

    private void saveIssuePriorities(Set<IssuePriority> issuePriorities) {
        List<IssuePriority> savedIssuePriorities = issuePriorityRepository.saveAll(issuePriorities);
        log.info("Successfully saved {} issue priorities", savedIssuePriorities.size());
    }

    private void saveIssueStates(Set<IssueState> issueStates) {
        List<IssueState> savedIssueStates = issueStateRepository.saveAll(issueStates);
        log.info("Successfully saved {} issue states", savedIssueStates.size());
    }

    public Map<String, IssuePriority> getAllIssuePrioritiesMap() {
        return issuePriorityRepository.findAll().stream()
                .collect(Collectors.toMap(IssuePriority::getId, Function.identity()));
    }

    public Map<String, IssueState> getAllIssueStatesMap() {
        return issueStateRepository.findAll().stream()
                .collect(Collectors.toMap(IssueState::getId, Function.identity()));
    }
}
