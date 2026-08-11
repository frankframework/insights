package org.frankframework.insights.businessvalue;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.issue.IssueNotFoundException;
import org.frankframework.insights.issue.IssueRepository;
import org.frankframework.insights.issue.IssueResponse;
import org.frankframework.insights.release.Release;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.frankframework.insights.release.ReleaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class BusinessValueService {

    private final BusinessValueRepository businessValueRepository;
    private final IssueRepository issueRepository;
    private final ReleaseRepository releaseRepository;
    private final Mapper mapper;

    public BusinessValueService(
            BusinessValueRepository businessValueRepository,
            IssueRepository issueRepository,
            ReleaseRepository releaseRepository,
            Mapper mapper) {
        this.businessValueRepository = businessValueRepository;
        this.issueRepository = issueRepository;
        this.releaseRepository = releaseRepository;
        this.mapper = mapper;
    }

    /**
     * Retrieves all business values for a specific release.
     * This is the primary way to fetch business values — they are release-scoped.
     * @param releaseId the ID of the release
     * @return set of business value responses for the release
     */
    @Transactional(readOnly = true)
    public List<BusinessValueResponse> getBusinessValuesByReleaseId(String releaseId) {
        log.info("Fetching business values for release with id: {}", releaseId);

        List<BusinessValue> businessValues = businessValueRepository.findByReleaseId(releaseId);
        log.info("Retrieved {} business values for release {}", businessValues.size(), releaseId);

        return businessValues.stream().map(this::mapToResponseWithIssues).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BusinessValueResponse getBusinessValueById(UUID id) throws BusinessValueNotFoundException {
        log.info("Fetching business value with id: {}", id);

        BusinessValue businessValue = findBusinessValueById(id);
        log.info(
                "Found business value '{}' with {} connected issues",
                businessValue.getTitle(),
                businessValue.getIssues().size());

        return mapToResponseWithIssues(businessValue);
    }

    @Transactional
    public BusinessValueResponse createBusinessValue(BusinessValueRequest request)
            throws BusinessValueAlreadyExistsException, ReleaseNotFoundException {
        log.info("Creating business value '{}' for release {}", request.title(), request.releaseId());

        Release release = findReleaseById(request.releaseId());

        Optional<BusinessValue> existing = businessValueRepository.findByTitleAndRelease(request.title(), release);
        if (existing.isPresent()) {
            throw new BusinessValueAlreadyExistsException(
                    "Business value with title '" + request.title() + "' already exists in this release");
        }

        BusinessValue businessValue = new BusinessValue();
        businessValue.setTitle(request.title());
        businessValue.setDescription(request.description());
        businessValue.setRelease(release);

        BusinessValue savedBusinessValue = businessValueRepository.save(businessValue);
        log.info(
                "Successfully created business value with id: {} and title: {}",
                savedBusinessValue.getId(),
                savedBusinessValue.getTitle());

        return mapToResponse(savedBusinessValue);
    }

    @Transactional
    public BusinessValueResponse updateBusinessValue(UUID id, UpdateBusinessValueRequest request)
            throws BusinessValueNotFoundException, BusinessValueAlreadyExistsException {
        log.info("Updating business value with id: {}", id);

        BusinessValue businessValue = findBusinessValueById(id);
        Release release = businessValue.getRelease();

        if (!businessValue.getTitle().equals(request.title())) {
            Optional<BusinessValue> existing = businessValueRepository.findByTitleAndRelease(request.title(), release);
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                log.warn("Business value with title '{}' already exists in this release", request.title());
                throw new BusinessValueAlreadyExistsException(
                        "Business value with title '" + request.title() + "' already exists in this release");
            }
        }

        businessValue.setTitle(request.title());
        businessValue.setDescription(request.description());
        BusinessValue updatedBusinessValue = businessValueRepository.save(businessValue);
        log.info("Successfully updated business value with id: {}", id);

        return mapToResponseWithIssues(updatedBusinessValue);
    }

    @Transactional
    public void deleteBusinessValue(UUID id) throws BusinessValueNotFoundException {
        log.info("Deleting business value with id: {}", id);

        BusinessValue businessValue = findBusinessValueById(id);

        if (businessValue.getIssues() != null && !businessValue.getIssues().isEmpty()) {
            log.info(
                    "Disconnecting {} issues from business value '{}' before deletion",
                    businessValue.getIssues().size(),
                    businessValue.getTitle());

            Set<Issue> issues = new HashSet<>(businessValue.getIssues());
            clearBusinessValueFromIssues(issues, businessValue);
            issueRepository.saveAll(issues);
        }

        businessValueRepository.delete(businessValue);
        log.info("Successfully deleted business value with id: {}", id);
    }

    @Transactional
    public BusinessValueResponse replaceIssueConnections(UUID businessValueId, ConnectIssuesRequest request)
            throws BusinessValueNotFoundException, IssueNotFoundException {
        log.info("Replacing issue connections for business value with id: {}", businessValueId);

        BusinessValue businessValue = findBusinessValueById(businessValueId);

        if (businessValue.getIssues() != null && !businessValue.getIssues().isEmpty()) {
            log.info(
                    "Disconnecting {} existing issues before connecting new ones",
                    businessValue.getIssues().size());
            Set<Issue> currentIssues = new HashSet<>(businessValue.getIssues());
            clearBusinessValueFromIssues(currentIssues, businessValue);
            issueRepository.saveAll(currentIssues);
            businessValue.getIssues().clear();
        }

        Set<Issue> newIssues = fetchAndValidateIssues(request.issueIds());
        log.info("Connecting {} issues to business value '{}'", newIssues.size(), businessValue.getTitle());
        setBusinessValueOnIssues(newIssues, businessValue);
        issueRepository.saveAll(newIssues);
        businessValue.getIssues().addAll(newIssues);

        return mapToResponseWithIssues(businessValue);
    }

    /**
     * Duplicates all business values from a source release into a target release.
     * Only copies title and description — no issue connections are carried over.
     * Business values whose title already exists in the target release are skipped.
     * @param targetReleaseId the release to copy business values into
     * @param request contains the sourceReleaseId to copy from
     * @return list of newly created business value responses
     */
    @Transactional
    public List<BusinessValueResponse> duplicateBusinessValues(
            String targetReleaseId, DuplicateBusinessValuesRequest request) throws ReleaseNotFoundException {
        log.info(
                "Duplicating business values from release {} to release {}",
                request.sourceReleaseId(),
                targetReleaseId);

        Release targetRelease = findReleaseById(targetReleaseId);
        List<BusinessValue> sourceBusinessValues = businessValueRepository.findByReleaseId(request.sourceReleaseId());

        Set<String> existingTitlesInTarget = businessValueRepository.findByReleaseId(targetReleaseId).stream()
                .map(BusinessValue::getTitle)
                .collect(Collectors.toSet());

        List<BusinessValue> duplicated = new ArrayList<>();
        for (BusinessValue source : sourceBusinessValues) {
            if (existingTitlesInTarget.contains(source.getTitle())) {
                log.info("Skipping duplicate of '{}' — title already exists in target release", source.getTitle());
                continue;
            }
            BusinessValue copy = new BusinessValue();
            copy.setTitle(source.getTitle());
            copy.setDescription(source.getDescription());
            copy.setRelease(targetRelease);
            duplicated.add(copy);
        }

        List<BusinessValue> saved = businessValueRepository.saveAll(duplicated);
        log.info("Duplicated {} business values to release {}", saved.size(), targetReleaseId);

        return saved.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private BusinessValue findBusinessValueById(UUID id) throws BusinessValueNotFoundException {
        return businessValueRepository
                .findById(id)
                .orElseThrow(() -> new BusinessValueNotFoundException("Business value with id " + id + " not found"));
    }

    private Release findReleaseById(String releaseId) throws ReleaseNotFoundException {
        return releaseRepository
                .findById(releaseId)
                .orElseThrow(() -> new ReleaseNotFoundException("Release with id " + releaseId + " not found", null));
    }

    private Set<Issue> fetchAndValidateIssues(Set<String> issueIds) throws IssueNotFoundException {
        Map<String, Optional<Issue>> issueMap =
                issueIds.stream().collect(Collectors.toMap(id -> id, issueRepository::findById));

        List<String> notFoundIds = issueMap.entrySet().stream()
                .filter(entry -> entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .toList();

        if (!notFoundIds.isEmpty()) {
            log.error("Issues not found: {}", notFoundIds);
            throw new IssueNotFoundException("Issues not found: " + String.join(", ", notFoundIds));
        }

        return issueMap.values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private void setBusinessValueOnIssues(Set<Issue> issues, BusinessValue businessValue) {
        issues.forEach(issue -> issue.setBusinessValue(businessValue));
    }

    private void clearBusinessValueFromIssues(Set<Issue> issues, BusinessValue businessValue) {
        issues.stream()
                .filter(issue -> businessValue.equals(issue.getBusinessValue()))
                .forEach(issue -> issue.setBusinessValue(null));
    }

    private BusinessValueResponse mapToResponse(BusinessValue businessValue) {
        return new BusinessValueResponse(
                businessValue.getId(),
                businessValue.getTitle(),
                businessValue.getDescription(),
                businessValue.getRelease().getId(),
                Set.of());
    }

    private BusinessValueResponse mapToResponseWithIssues(BusinessValue businessValue) {
        Set<IssueResponse> issueResponses =
                (businessValue.getIssues() != null && !businessValue.getIssues().isEmpty())
                        ? businessValue.getIssues().stream()
                                .map(issue -> mapper.toDTO(issue, IssueResponse.class))
                                .collect(Collectors.toSet())
                        : Set.of();

        return new BusinessValueResponse(
                businessValue.getId(),
                businessValue.getTitle(),
                businessValue.getDescription(),
                businessValue.getRelease().getId(),
                issueResponses);
    }
}
