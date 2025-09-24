package org.frankframework.webapp.branch;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.shared.entity.Branch;
import org.frankframework.webapp.common.configuration.properties.GitHubProperties;
import org.frankframework.webapp.common.entityconnection.branchpullrequest.BranchPullRequest;
import org.frankframework.webapp.common.entityconnection.branchpullrequest.BranchPullRequestRepository;
import org.frankframework.webapp.common.mapper.Mapper;
import org.frankframework.webapp.github.GitHubClient;
import org.frankframework.webapp.github.GitHubRepositoryStatisticsService;
import org.springframework.stereotype.Service;

/**
 * Service class for managing branches.
 * Handles the injection, mapping, and processing of GitHub branches into the database.
 */
@Service
@Slf4j
public class BranchService {
    private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;
    private final GitHubClient gitHubClient;
    private final Mapper mapper;
    private final BranchRepository branchRepository;
    private final List<String> branchProtectionRegexes;
    private final BranchPullRequestRepository branchPullRequestRepository;

    public BranchService(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            GitHubClient gitHubClient,
            Mapper mapper,
            BranchRepository branchRepository,
            GitHubProperties gitHubProperties,
            BranchPullRequestRepository branchPullRequestRepository) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.branchRepository = branchRepository;
        this.branchProtectionRegexes = gitHubProperties.getBranchProtectionRegexes();
        this.branchPullRequestRepository = branchPullRequestRepository;
    }

    /**
     * Injects branches from GitHub into the database.
     * @throws BranchInjectionException if an error occurs during the injection process
     */
    public void injectBranches() throws BranchInjectionException {
        if (gitHubRepositoryStatisticsService
                        .getGitHubRepositoryStatisticsDTO()
                        .getGitHubBranchCount(branchProtectionRegexes)
                == branchRepository.count()) {
            log.info("Branches already found in the in database");
            return;
        }

        try {
            log.info("Amount of branches found in database: {}", branchRepository.count());
            log.info(
                    "Amount of branches found in GitHub: {}",
                    gitHubRepositoryStatisticsService
                            .getGitHubRepositoryStatisticsDTO()
                            .getGitHubBranchCount(branchProtectionRegexes));

            log.info("Start injecting GitHub branches");
            Set<BranchDTO> branchDTOs = gitHubClient.getBranches();
            Set<Branch> branches = findProtectedBranchesByRegexPattern(branchDTOs);

            if (!branches.isEmpty()) {
                saveBranches(branches);
            }
        } catch (Exception e) {
            throw new BranchInjectionException("Error while injecting GitHub branches", e);
        }
    }

    /**
     * Finds protected branches by regex patterns and maps them to database entities.
     * @param branchDTOs the set of branch DTOs to filter
     * @return a set of filtered and mapped Branch entities
     */
    private Set<Branch> findProtectedBranchesByRegexPattern(Set<BranchDTO> branchDTOs) {
        log.info("Find protected branches by patterns: {}, and map them to database entities", branchProtectionRegexes);
        Set<Branch> filteredBranches = branchDTOs.stream()
                .filter(branchDTO -> branchProtectionRegexes.stream()
                        .anyMatch(regex ->
                                Pattern.compile(regex).matcher(branchDTO.name()).find()))
                .map(branchDTO -> mapper.toEntity(branchDTO, Branch.class))
                .collect(Collectors.toSet());

        log.info("Successfully filtered {} branches by protection patterns and mapped them.", filteredBranches.size());
        return filteredBranches;
    }

    /**
     * Fetches all branches from the database.
     * @return a list of all Branch entities
     */
    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    /**
     * Fetches all branch pull requests by branch
     * @param branches the list of branches to fetch pull requests for
     * @return a map of branch IDs to sets of BranchPullRequest entities
     */
    public Map<String, Set<BranchPullRequest>> getBranchPullRequestsByBranches(List<Branch> branches) {
        return branches.stream()
                .collect(Collectors.toMap(
                        Branch::getId, branch -> branchPullRequestRepository.findAllByBranch_Id(branch.getId())));
    }

    /**
     * Fetches all branch pull requests by branch ID
     * @param id the ID of the branch
     * @return a set of BranchPullRequest entities associated with the branch ID
     */
    public Set<BranchPullRequest> getBranchPullRequestsByBranchId(String id) {
        return branchPullRequestRepository.findAllByBranch_Id(id);
    }

    /**
     * Saves a set of branches to the database.
     * @param branches the set of branches to save
     */
    public void saveBranches(Set<Branch> branches) {
        List<Branch> savedBranches = branchRepository.saveAll(branches);
        log.info("Successfully saved {} branches.", savedBranches.size());
    }
}
