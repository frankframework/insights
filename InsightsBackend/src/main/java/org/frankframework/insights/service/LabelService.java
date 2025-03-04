package org.frankframework.insights.service;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.LabelDTO;
import org.frankframework.insights.mapper.LabelMapper;
import org.frankframework.insights.models.Label;
import org.frankframework.insights.repository.LabelRepository;
import org.springframework.stereotype.Service;

@Service
public class LabelService {

    private final GitHubClient gitHubClient;

    private final LabelMapper labelMapper;

    private final LabelRepository labelRepository;

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
			Set<LabelDTO> labelDTOs = gitHubClient.getLabels();

			// Create an ObjectMapper instance
			ObjectMapper objectMapper = new ObjectMapper();

			// Convert labelDTOs to JSON string
			System.out.println("Received LabelDTOs:");
			System.out.println(objectMapper.writeValueAsString(labelDTOs));

			Set<Label> labels = labelMapper.toEntity(labelDTOs);

			// Convert labels to JSON string
			System.out.println("Mapped Labels:");
			System.out.println(objectMapper.writeValueAsString(labels));

			saveLabels(labels);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	private void saveLabels(Set<Label> labels) {
        labelRepository.saveAll(labels);
    }
}
