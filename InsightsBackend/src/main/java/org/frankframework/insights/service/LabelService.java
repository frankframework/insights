package org.frankframework.insights.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.Set;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.mapper.LabelMapper;
import org.frankframework.insights.models.Label;
import org.frankframework.insights.repository.LabelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LabelService {

    private final GitHubClient gitHubClient;

    private final LabelMapper labelMapper;

    private final LabelRepository labelRepository;

    @Autowired
    public LabelService(GitHubClient gitHubClient, LabelMapper labelMapper, LabelRepository labelRepository) {
        this.gitHubClient = gitHubClient;
        this.labelMapper = labelMapper;
        this.labelRepository = labelRepository;
    }

    public void injectLabels() throws RuntimeException {
        if (!labelRepository.findAll().isEmpty()) {
            return;
        }

        try {
            JsonNode jsonLabels = gitHubClient.getLabels();

            Set<Label> labels = labelMapper.jsonToLabels(jsonLabels);

            saveLabels(labels);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public Set<Label> getAllLabelsByName(Set<String> names) {
        if (names.isEmpty()) {
            return new HashSet<>();
        }

        return labelRepository.findByNameIn(names);
    }

    @Transactional
    private void saveLabels(Set<Label> labels) {
        labelRepository.saveAll(labels);
    }
}
