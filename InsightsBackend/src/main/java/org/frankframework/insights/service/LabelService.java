package org.frankframework.insights.service;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.LabelDTO;
import org.frankframework.insights.exceptions.labels.LabelDatabaseException;
import org.frankframework.insights.exceptions.labels.LabelInjectionException;
import org.frankframework.insights.mapper.Mapper;
import org.frankframework.insights.models.Label;
import org.frankframework.insights.repository.LabelRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LabelService {

    private final GitHubClient gitHubClient;

    private final Mapper labelMapper;

    private final LabelRepository labelRepository;

    public LabelService(GitHubClient gitHubClient, Mapper labelMapper, LabelRepository labelRepository) {
        this.gitHubClient = gitHubClient;
        this.labelMapper = labelMapper;
        this.labelRepository = labelRepository;
    }

    public void injectLabels() throws LabelInjectionException {
        if (!labelRepository.findAll().isEmpty()) {
            log.info("Labels already found in the in database");
            return;
        }

        try {
            log.info("Start injecting GitHub labels");
            Set<LabelDTO> labelDTOs = gitHubClient.getLabels();
            Set<Label> labels = labelMapper.toEntity(labelDTOs, Label.class);
            saveLabels(labels);
        } catch (Exception e) {
            throw new LabelInjectionException("Error while injecting GitHub labels", e);
        }
    }

    private void saveLabels(Set<Label> labels) throws LabelDatabaseException {
        try {
            labelRepository.saveAll(labels);
            log.info("Successfully saved labels");
        } catch (Exception e) {
            throw new LabelDatabaseException("Error while saving labels", e);
        }
    }
}
