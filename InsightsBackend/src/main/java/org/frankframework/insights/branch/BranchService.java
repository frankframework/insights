package org.frankframework.insights.branch;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.configuration.GitHubProperties;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BranchService {
    private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;
    private final GitHubClient gitHubClient;
    private final Mapper mapper;
    private final BranchRepository branchRepository;
    private final List<String> branchProtectionRegexes;

    public BranchService(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            GitHubClient gitHubClient,
            Mapper mapper,
            BranchRepository branchRepository,
            GitHubProperties gitHubProperties) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.branchRepository = branchRepository;
        this.branchProtectionRegexes = gitHubProperties.getBranchProtectionRegexes();
    }

    public void injectBranches() throws BranchInjectionException {
        if (gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubBranchCount(branchProtectionRegexes)
                == branchRepository.count()) {
            log.info("Branches already found in the in database");
            return;
        }

        try {
            log.info("Start injecting GitHub branches");
            Set<BranchDTO> branchDTOs = gitHubClient.getBranches();
            Set<Branch> branches = filterBranchesByProtectionPattern(branchDTOs);
            saveBranches(branches);
        } catch (Exception e) {
            throw new BranchInjectionException("Error while injecting GitHub branches", e);
        }
    }

    public boolean doesBranchContainCommit(Branch branch, String commitOid) {
        boolean containsCommit =
                branch.getCommits().stream().anyMatch(commit -> commit.getOid().equals(commitOid));

        log.info("Branch {} contains commit: {}", branch.getName(), containsCommit);

        return containsCommit;
    }

    private Set<Branch> filterBranchesByProtectionPattern(Set<BranchDTO> branchDTOs) {
        log.info(
                "Filtering branches by protection patterns: {}, and maps them to database entities",
                branchProtectionRegexes);
        Set<Branch> filteredBranches = branchDTOs.stream()
                .filter(branchDTO -> branchProtectionRegexes.stream().anyMatch(regex -> Pattern.compile(regex)
                        .matcher(branchDTO.getName())
                        .find()))
                .map(branchDTO -> mapper.toEntity(branchDTO, Branch.class))
                .collect(Collectors.toSet());

        log.info("Successfully filtered {} branches by protection patterns and mapped them.", filteredBranches.size());
        return filteredBranches;
    }

    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    public void saveBranches(Set<Branch> branches) {
        branchRepository.saveAll(branches);
        log.info("Successfully saved {} branches.", branches.size());
    }
}
