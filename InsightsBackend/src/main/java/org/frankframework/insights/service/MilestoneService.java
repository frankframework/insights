package org.frankframework.insights.service;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.MilestoneDTO;
import org.frankframework.insights.exceptions.milestones.MilestoneInjectionException;
import org.frankframework.insights.mapper.Mapper;
import org.frankframework.insights.models.Milestone;
import org.frankframework.insights.repository.MilestoneRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MilestoneService {

    private final RepositoryStatisticsService repositoryStatisticsService;

    private final GitHubClient gitHubClient;

    private final Mapper mapper;

    private final MilestoneRepository milestoneRepository;

    public MilestoneService(
            RepositoryStatisticsService repositoryStatisticsService,
            GitHubClient gitHubClient,
            Mapper mapper,
            MilestoneRepository milestoneRepository) {
        this.repositoryStatisticsService = repositoryStatisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.milestoneRepository = milestoneRepository;
    }

    public void injectMilestones() throws MilestoneInjectionException {
        if (repositoryStatisticsService.getRepositoryStatisticsDTO().milestoneCount() == milestoneRepository.count()) {
            log.info("Milestones already found in the in database");
            return;
        }

        try {
            log.info("Start injecting GitHub milestones");
            Set<MilestoneDTO> milestoneDTOS = gitHubClient.getMilestones();
            Set<Milestone> milestones = mapper.toEntity(milestoneDTOS, Milestone.class);
            saveMilestones(milestones);
        } catch (Exception e) {
            throw new MilestoneInjectionException("Error while injecting GitHub milestones", e);
        }
    }

    private void saveMilestones(Set<Milestone> milestones) {
        milestoneRepository.saveAll(milestones);
        log.info("Successfully saved milestones");
    }
}
