package org.frankframework.insights.service;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.LabelDTO;
import org.frankframework.insights.exceptions.labels.LabelInjectionException;
import org.frankframework.insights.mapper.Mapper;
import org.frankframework.insights.models.Label;
import org.frankframework.insights.repository.LabelRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LabelService {

    private final RepositoryStatisticsService repositoryStatisticsService;

    private final GitHubClient gitHubClient;

    private final Mapper mapper;

    private final LabelRepository labelRepository;

    public LabelService(
            RepositoryStatisticsService repositoryStatisticsService,
            GitHubClient gitHubClient,
            Mapper mapper,
            LabelRepository labelRepository) {
        this.repositoryStatisticsService = repositoryStatisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.labelRepository = labelRepository;
    }

    public void injectLabels() throws LabelInjectionException {
        if (repositoryStatisticsService.getRepositoryStatisticsDTO().labelCount() == labelRepository.count()) {
            log.info("Labels already found in the in database");
            return;
        }

        try {
            log.info("Start injecting GitHub labels");
            Set<LabelDTO> labelDTOs = gitHubClient.getLabels();
            Set<Label> labels = mapper.toEntity(labelDTOs, Label.class);
            saveLabels(labels);
        } catch (Exception e) {
            throw new LabelInjectionException("Error while injecting GitHub labels", e);
        }
    }

    private void saveLabels(Set<Label> labels) {
        labelRepository.saveAll(labels);
        log.info("Successfully saved labels");
    }
}
