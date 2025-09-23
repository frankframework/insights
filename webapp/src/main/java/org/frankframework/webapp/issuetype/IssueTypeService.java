package org.frankframework.webapp.issuetype;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.webapp.common.mapper.Mapper;
import org.frankframework.webapp.github.GitHubClient;
import org.frankframework.webapp.github.GitHubRepositoryStatisticsService;
import org.springframework.stereotype.Service;

/**
 * Service class for managing issue types.
 * Handles the injection, mapping, and processing of GitHub issue types into the database.
 */
@Service
@Slf4j
public class IssueTypeService {

    private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;
    private final IssueTypeRepository issueTypeRepository;
    private final GitHubClient gitHubClient;
    private final Mapper mapper;

    public IssueTypeService(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            IssueTypeRepository issueTypeRepository,
            GitHubClient gitHubClient,
            Mapper mapper) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.issueTypeRepository = issueTypeRepository;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
    }

    /**
     * Injects issue types from GitHub into the database.
     * @throws IssueTypeInjectionException if an error occurs during the injection process
     */
    public void injectIssueTypes() throws IssueTypeInjectionException {
        if (gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubIssueTypeCount()
                == issueTypeRepository.count()) {
            log.info("Issue types already found in the in database");
            return;
        }

        try {
            log.info("Amount of issueTypes found in database: {}", issueTypeRepository.count());
            log.info(
                    "Amount of issue types found in GitHub: {}",
                    gitHubRepositoryStatisticsService
                            .getGitHubRepositoryStatisticsDTO()
                            .getGitHubIssueTypeCount());

            log.info("Start injecting GitHub issue types");
            Set<IssueTypeDTO> issueTypeDTOS = gitHubClient.getIssueTypes();
            Set<IssueType> issueTypes = mapper.toEntity(issueTypeDTOS, IssueType.class);
            saveIssueTypes(issueTypes);
        } catch (Exception e) {
            throw new IssueTypeInjectionException("Error while injecting GitHub issue types", e);
        }
    }

    /**
     * Get all issue types from the database
     * @return a map of issue type id to issue type
     */
    public Map<String, IssueType> getAllIssueTypesMap() {
        return issueTypeRepository.findAll().stream().collect(Collectors.toMap(IssueType::getId, Function.identity()));
    }

    /**
     * Saves the provided issue types to the database.
     * @param issueTypes the set of issue types to save
     */
    private void saveIssueTypes(Set<IssueType> issueTypes) {
        List<IssueType> savedIssueTypes = issueTypeRepository.saveAll(issueTypes);
        log.info("Successfully saved {} issue types", savedIssueTypes.size());
    }
}
