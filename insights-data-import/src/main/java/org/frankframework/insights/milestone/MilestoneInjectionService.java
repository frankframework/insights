package org.frankframework.insights.milestone;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.graphql.GitHubGraphQLClient;
import org.springframework.stereotype.Service;

/**
 * Service class for injecting milestones.
 * Handles the injection, mapping, and processing of GitHub milestones into the database.
 */
@Service
@Slf4j
public class MilestoneInjectionService {

    private final GitHubGraphQLClient gitHubGraphQLClient;

    private final Mapper mapper;

    private final MilestoneRepository milestoneRepository;

    public MilestoneInjectionService(
            GitHubGraphQLClient gitHubGraphQLClient, Mapper mapper, MilestoneRepository milestoneRepository) {
        this.gitHubGraphQLClient = gitHubGraphQLClient;
        this.mapper = mapper;
        this.milestoneRepository = milestoneRepository;
    }

    /**
     * Injects GitHub milestones into the database.
     * @throws MilestoneInjectionException if an error occurs during the injection process.
     */
    public void injectMilestones() throws MilestoneInjectionException {
        try {
            log.info("Start injecting GitHub milestones");
            Set<MilestoneDTO> milestoneDTOS = gitHubGraphQLClient.getMilestones();
            Set<Milestone> milestones = mapper.toEntity(milestoneDTOS, Milestone.class);
            saveMilestones(milestones);
        } catch (Exception e) {
            throw new MilestoneInjectionException("Error while injecting GitHub milestones", e);
        }
    }

    /**
     * Fetches all milestones from the database and returns them as a map.
     * Used while injecting issues and pull requests to resolve their milestone references.
     * @return a map of milestone IDs to Milestone objects
     */
    public Map<String, Milestone> getAllMilestonesMap() {
        return milestoneRepository.findAll().stream().collect(Collectors.toMap(Milestone::getId, Function.identity()));
    }

    /**
     * Saves a set of milestones to the database.
     * @param milestones the set of milestones to save
     */
    private void saveMilestones(Set<Milestone> milestones) {
        List<Milestone> savedMilestones = milestoneRepository.saveAll(milestones);
        log.info("Successfully saved {} milestones", savedMilestones.size());
    }
}
