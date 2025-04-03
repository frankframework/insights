package org.frankframework.insights.label;

import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LabelService {

    private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;

    private final GitHubClient gitHubClient;

    private final Mapper mapper;

    private final LabelRepository labelRepository;

    public LabelService(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            GitHubClient gitHubClient,
            Mapper mapper,
            LabelRepository labelRepository) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.labelRepository = labelRepository;
    }

    public void injectLabels() throws LabelInjectionException {
        if (gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubLabelCount()
                == labelRepository.count()) {
            log.info("Labels already found in the in database");
            return;
        }

        try {
			log.info("Amount of labels found in database: {}", labelRepository.count());
			log.info("Amount of labels found in GitHub: {}", gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubLabelCount());

			log.info("Start injecting GitHub labels");
            Set<LabelDTO> labelDTOs = gitHubClient.getLabels();
            Set<Label> labels = mapper.toEntity(labelDTOs, Label.class);
            saveLabels(labels);
        } catch (Exception e) {
            throw new LabelInjectionException("Error while injecting GitHub labels", e);
        }
    }

	public List<Label> getAllLabels() {
		return labelRepository.findAll();
	}

    private void saveLabels(Set<Label> labels) {
        List<Label> savedLabels = labelRepository.saveAll(labels);
        log.info("Successfully saved {} labels", savedLabels.size());
    }
}
