package org.frankframework.insights.service;

import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.Set;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.LabelDTO;
import org.frankframework.insights.mapper.Mapper;
import org.frankframework.insights.models.Label;
import org.frankframework.insights.repository.LabelRepository;
import org.springframework.stereotype.Service;

@Service
public class LabelService {

    private final GitHubClient gitHubClient;

    private final Mapper<LabelDTO, Label> labelMapper;

    private final LabelRepository labelRepository;

    public LabelService(GitHubClient gitHubClient, Mapper<LabelDTO, Label> labelMapper, LabelRepository labelRepository) {
        this.gitHubClient = gitHubClient;
        this.labelMapper = labelMapper;
        this.labelRepository = labelRepository;
    }

	@Transactional
    public void injectLabels() throws RuntimeException {
        if (!labelRepository.findAll().isEmpty()) {
            return;
        }

        try {
            Set<LabelDTO> labelDTOs = gitHubClient.getLabels();

			Set<Label> labels = labelMapper.toEntity(labelDTOs, Label.class);

            saveLabels(labels);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Set<Label> getAllLabelsByName(Set<String> names) {
        if (names.isEmpty()) {
            return new HashSet<>();
        }

        return labelRepository.findByNameIn(names);
    }

    private void saveLabels(Set<Label> labels) {
        labelRepository.saveAll(labels);
    }
}
