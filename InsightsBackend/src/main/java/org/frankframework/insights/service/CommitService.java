package org.frankframework.insights.service;

import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.CommitDTO;
import org.frankframework.insights.dto.LabelDTO;
import org.frankframework.insights.mapper.CommitMapper;
import org.frankframework.insights.mapper.LabelMapper;
import org.frankframework.insights.models.Commit;
import org.frankframework.insights.models.Label;
import org.frankframework.insights.repository.CommitRepository;
import org.frankframework.insights.repository.LabelRepository;

import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class CommitService {
	private final GitHubClient gitHubClient;

	private final CommitMapper commitMapper;

	private final CommitRepository commitRepository;

	public CommitService(GitHubClient gitHubClient, CommitMapper commitMapper, CommitRepository commitRepository) {
		this.gitHubClient = gitHubClient;
		this.commitMapper = commitMapper;
		this.commitRepository = commitRepository
	;
	}

	public void injectCommits() throws RuntimeException {
		if (!commitRepository.findAll().isEmpty()) {
			return;
		}

		try {
//			Set<CommitDTO> commitDTOS = gitHubClient.getCommits();
//
//			Set<Commit> commits = commitMapper.toEntity(commitDTOS);
//
//			saveCommits(commits);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	private void saveCommits(Set<Commit> commits) {
		commitRepository.saveAll(commits);
	}
}
