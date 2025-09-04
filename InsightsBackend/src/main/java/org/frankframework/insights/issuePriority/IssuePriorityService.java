package org.frankframework.insights.issuePriority;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.configuration.properties.GitHubProperties;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubPrioritySingleSelectDTO;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IssuePriorityService {
    private final IssuePriorityRepository issuePriorityRepository;
    private final GitHubClient gitHubClient;
    private final Mapper mapper;
    private final String projectId;

    private static final String PRIORITY_FIELD_NAME = "priority";

    public IssuePriorityService(
            IssuePriorityRepository issuePriorityRepository,
            GitHubClient gitHubClient,
            Mapper mapper,
            GitHubProperties gitHubProperties) {
        this.issuePriorityRepository = issuePriorityRepository;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.projectId = gitHubProperties.getProjectId();
    }

    public void injectIssuePriorities() throws IssuePriorityInjectionException {
        if (issuePriorityRepository.count() != 0) {
            log.info("Issue priorities already found in the in database");
            return;
        }

        try {
            log.info("Start injecting GitHub issue priorities");
            Set<GitHubPrioritySingleSelectDTO.SingleSelectObject> singleSelectDTOS =
                    gitHubClient.getIssuePriorities(projectId);
            Set<IssuePriority> issuePriorities = fetchAndMapGithubPriorityOptions(singleSelectDTOS);
            saveIssuePriorities(issuePriorities);
        } catch (Exception e) {
            throw new IssuePriorityInjectionException("Error while injecting GitHub issue priorities", e);
        }
    }

    private Set<IssuePriority> fetchAndMapGithubPriorityOptions(
            Set<GitHubPrioritySingleSelectDTO.SingleSelectObject> singleSelectObjects) {
        return singleSelectObjects.stream()
                .filter(dto -> dto.name() != null)
                .filter(dto -> PRIORITY_FIELD_NAME.equalsIgnoreCase(dto.name()))
                .filter(dto -> dto.options() != null && !dto.options().isEmpty())
                .findFirst()
                .map(this::mapPriorityOptionsToEntities)
                .orElseGet(Set::of);
    }

    private Set<IssuePriority> mapPriorityOptionsToEntities(
            GitHubPrioritySingleSelectDTO.SingleSelectObject priorityDTO) {
        if (priorityDTO.options() == null) return Set.of();
        return priorityDTO.options().stream()
                .map(option -> mapper.toEntity(option, IssuePriority.class))
                .collect(Collectors.toSet());
    }

    private void saveIssuePriorities(Set<IssuePriority> issuePriorities) {
        List<IssuePriority> savedIssuePriorities = issuePriorityRepository.saveAll(issuePriorities);
        log.info("Successfully saved {} issue priorities", savedIssuePriorities.size());
    }

    public Map<String, IssuePriority> getAllIssuePrioritiesMap() {
        return issuePriorityRepository.findAll().stream()
                .collect(Collectors.toMap(IssuePriority::getId, Function.identity()));
    }
}
