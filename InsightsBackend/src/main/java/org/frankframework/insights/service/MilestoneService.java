package org.frankframework.insights.service;

import java.util.Set;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.MilestoneDTO;
import org.frankframework.insights.exceptions.milestones.MilestoneDatabaseException;
import org.frankframework.insights.exceptions.milestones.MilestoneMappingException;
import org.frankframework.insights.mapper.MilestoneMapper;
import org.frankframework.insights.models.Milestone;
import org.frankframework.insights.repository.MilestoneRepository;
import org.springframework.stereotype.Service;

@Service
public class MilestoneService {

    private final GitHubClient gitHubClient;

    private final MilestoneMapper milestoneMapper;

    private final MilestoneRepository milestoneRepository;

    public MilestoneService(
            GitHubClient gitHubClient, MilestoneMapper milestoneMapper, MilestoneRepository milestoneRepository) {
        this.gitHubClient = gitHubClient;
        this.milestoneMapper = milestoneMapper;
        this.milestoneRepository = milestoneRepository;
    }

    public void injectMilestones() throws MilestoneMappingException {
        if (!milestoneRepository.findAll().isEmpty()) {
            return;
        }

        try {
            Set<MilestoneDTO> milestoneDTOS = gitHubClient.getMilestones();

            Set<Milestone> milestones = milestoneMapper.toEntity(milestoneDTOS);

            saveMilestones(milestones);
        } catch (Exception e) {
            throw new MilestoneMappingException();
        }
    }

    private void saveMilestones(Set<Milestone> milestones) throws MilestoneDatabaseException {
        try {
            milestoneRepository.saveAll(milestones);
        } catch (Exception e) {
            throw new MilestoneDatabaseException();
        }
    }
}
