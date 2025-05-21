package org.frankframework.insights.milestone;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubPropertyState;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.springframework.stereotype.Service;

/**
 * Service class for managing milestones.
 * Handles the injection, mapping, and processing of GitHub milestones into the database.
 */
@Service
@Slf4j
public class MilestoneService {

    private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;

    private final GitHubClient gitHubClient;

    private final Mapper mapper;

    private final MilestoneRepository milestoneRepository;

    public MilestoneService(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            GitHubClient gitHubClient,
            Mapper mapper,
            MilestoneRepository milestoneRepository) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.milestoneRepository = milestoneRepository;
    }

    /**
     * Injects GitHub milestones into the database.
     * @throws MilestoneInjectionException if an error occurs during the injection process.
     */
    public void injectMilestones() throws MilestoneInjectionException {
        if (gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubMilestoneCount()
                == milestoneRepository.count()) {
            log.info("Milestones already found in the in database");
            return;
        }

        try {
            log.info("Amount of milestones found in database: {}", milestoneRepository.count());
            log.info(
                    "Amount of milestones found in GitHub: {}",
                    gitHubRepositoryStatisticsService
                            .getGitHubRepositoryStatisticsDTO()
                            .getGitHubMilestoneCount());

            log.info("Start injecting GitHub milestones");
            Set<MilestoneDTO> milestoneDTOS = gitHubClient.getMilestones();
            Set<Milestone> milestones = mapper.toEntity(milestoneDTOS, Milestone.class);
            saveMilestones(milestones);
        } catch (Exception e) {
            throw new MilestoneInjectionException("Error while injecting GitHub milestones", e);
        }
    }

    /**
     * Fetches all open milestones from the database and returns them as a set of MilestoneResponse objects.
     * @return a set of all open milestones
     * @throws MappingException if an error occurs during the mapping process
     */
    public Set<MilestoneResponse> getAllOpenMilestones() throws MappingException {
        Set<Milestone> openMilestones = milestoneRepository.findAllByState(GitHubPropertyState.OPEN);
        log.info("Successfully fetched {} open milestones from database", openMilestones.size());
        return mapper.toDTO(openMilestones, MilestoneResponse.class);
    }

    /**
     * Fetches all milestones from the database and returns them as a map.
     * @return a map of all milestones, where the key is the milestone ID and the value is the Milestone object
     */
    public Map<String, Milestone> getAllMilestonesMap() {
        return milestoneRepository.findAll().stream()
                .collect(Collectors.toMap(Milestone::getId, milestone -> milestone));
    }

    /**
     * Saves a set of milestones to the database.
     * @param milestones the set of milestones to save
     */
    private void saveMilestones(Set<Milestone> milestones) {
        List<Milestone> savedMilestones = milestoneRepository.saveAll(milestones);
        log.info("Successfully saved {} milestones", savedMilestones.size());
    }

    /**
     * Checks if a milestone with the given ID exists in the database.
     * @param milestoneId the ID of the milestone to check
     * @return the Milestone object if it exists
     * @throws MilestoneNotFoundException if the milestone does not exist
     */
    public Milestone checkIfMilestoneExists(String milestoneId) throws MilestoneNotFoundException {
        Optional<Milestone> milestone = milestoneRepository.findById(milestoneId);
        if (milestone.isEmpty()) {
            throw new MilestoneNotFoundException("Milestone with ID [" + milestoneId + "] not found.", null);
        }

        return milestone.get();
    }
}
