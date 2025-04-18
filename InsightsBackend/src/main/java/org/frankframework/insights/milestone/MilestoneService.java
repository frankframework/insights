package org.frankframework.insights.milestone;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.springframework.stereotype.Service;

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

    public Map<String, Milestone> getAllMilestonesMap() {
        return milestoneRepository.findAll().stream()
                .collect(Collectors.toMap(Milestone::getId, milestone -> milestone));
    }

    private void saveMilestones(Set<Milestone> milestones) {
        List<Milestone> savedMilestones = milestoneRepository.saveAll(milestones);
        log.info("Successfully saved {} milestones", savedMilestones.size());
    }
}
