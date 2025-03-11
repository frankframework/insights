package org.frankframework.insights.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.ReleaseDTO;
import org.frankframework.insights.dto.TagCommitDTO;
import org.frankframework.insights.exceptions.clients.GitHubClientException;
import org.frankframework.insights.exceptions.releases.ReleaseInjectionException;
import org.frankframework.insights.mapper.Mapper;
import org.frankframework.insights.models.Branch;
import org.frankframework.insights.models.Commit;
import org.frankframework.insights.models.Release;
import org.frankframework.insights.repository.ReleaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReleaseServiceTest {

	@Mock
	private GitHubClient gitHubClient;

	@Mock
	private Mapper releaseMapper;

	@Mock
	private ReleaseRepository releaseRepository;

	@Mock
	private BranchService branchService;

	@InjectMocks
	private ReleaseService releaseService;

	private ReleaseDTO mockReleaseDTO;
    private Release mockRelease;
	private Branch mockBranch;

    @BeforeEach
	void setUp() {
        Commit mockCommit = new Commit();
		mockCommit.setOid("sha123");

        TagCommitDTO mockTagCommitDTO = new TagCommitDTO();
		mockTagCommitDTO.setOid("sha123");

		mockReleaseDTO = new ReleaseDTO();
		mockReleaseDTO.setTagName("v9.0");
		mockReleaseDTO.setTagCommit(mockTagCommitDTO);

		mockRelease = new Release();
		mockRelease.setTagName("v9.0");
		mockRelease.setCommits(new HashSet<>(List.of(mockCommit)));

		mockBranch = new Branch();
		mockBranch.setName("master");
	}

	@Test
	public void should_InjectReleases_when_BranchMatchesWithCommits() throws Exception {
		when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));
		when(releaseRepository.findAll()).thenReturn(Collections.emptyList());
		when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
		when(branchService.doesBranchContainCommit(mockBranch, mockReleaseDTO.getTagCommit().getOid())).thenReturn(true);
		when(releaseMapper.toEntity(any(), any())).thenReturn(Set.of(mockRelease));

		releaseService.injectReleases();

		verify(releaseRepository, times(1)).save(any(Release.class));
	}

	@Test
	public void should_NotInjectReleases_when_BranchIsNoMatching() throws Exception {
		when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));
		when(releaseRepository.findAll()).thenReturn(Collections.emptyList());
		when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
		when(branchService.doesBranchContainCommit(any(), any())).thenReturn(false);

		releaseService.injectReleases();

		verify(releaseRepository, times(0)).save(any(Release.class));
	}

	@Test
	public void should_NotInjectReleases_when_DatabaseIsAlreadyFilled() throws Exception {
		when(releaseRepository.findAll()).thenReturn(List.of(mockRelease));

		releaseService.injectReleases();

		verify(gitHubClient, times(0)).getReleases();
		verify(releaseRepository, times(0)).save(any(Release.class));
	}

	@Test
	public void should_InjectReleases_when_MultipleBranchesAreMatching() throws Exception {
		Branch developmentBranch = new Branch();
		developmentBranch.setName("development");

		when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));
		when(releaseRepository.findAll()).thenReturn(Collections.emptyList());
		when(branchService.getAllBranches()).thenReturn(List.of(mockBranch, developmentBranch));
		when(branchService.doesBranchContainCommit(any(), any())).thenReturn(true);
		when(releaseMapper.toEntity(any(), any())).thenReturn(Set.of(mockRelease));

		releaseService.injectReleases();

		verify(releaseRepository, times(1)).save(any(Release.class));
	}

	@Test
	public void should_NotInjectReleases_when_ReleaseBranchesMapIsEmpty() throws Exception {
		when(gitHubClient.getReleases()).thenReturn(Collections.emptySet());

		releaseService.injectReleases();

		verify(releaseRepository, times(0)).save(any(Release.class));
	}

	@Test
	public void should_ThrowInjectionException_when_GitHubIsRaisingAnError() throws GitHubClientException {
		when(gitHubClient.getReleases()).thenThrow(new GitHubClientException("Error fetching releases from GitHub", null));

		assertThrows(ReleaseInjectionException.class, () -> releaseService.injectReleases());
	}
}
