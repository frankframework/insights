package org.frankframework.insights.businessvalue;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.issue.IssueNotFoundException;
import org.frankframework.insights.issue.IssueRepository;
import org.frankframework.insights.issue.IssueResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class BusinessValueService {

    private final BusinessValueRepository businessValueRepository;
    private final IssueRepository issueRepository;
    private final Mapper mapper;

    public BusinessValueService(
            BusinessValueRepository businessValueRepository, IssueRepository issueRepository, Mapper mapper) {
        this.businessValueRepository = businessValueRepository;
        this.issueRepository = issueRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Set<BusinessValueResponse> getAllBusinessValues() {
        log.info("Fetching all business values");

        List<BusinessValue> businessValues = businessValueRepository.findAll();
        log.info("Found {} business values", businessValues.size());

        return businessValues.stream().map(this::mapToResponseWithIssues).collect(Collectors.toSet());
    }

    /**
     * Retrieves all unique business values associated with issues in a specific release.
     * This is a public endpoint that doesn't require authentication.
     * Uses IssueRepository to get issues by release, then collects their business values.
     * @param releaseId the ID of the release
     * @return set of business value responses associated with the release's issues
     */
    @Transactional(readOnly = true)
    public Set<BusinessValueResponse> getBusinessValuesByReleaseId(String releaseId) {
        log.info("Fetching business values for release with id: {}", releaseId);

        Set<Issue> allIssues = issueRepository.findIssuesByReleaseId(releaseId);
        Set<String> businessValueNames = extractBusinessValueNames(allIssues);
        Set<BusinessValue> businessValues = fetchBusinessValuesByNames(businessValueNames);

        log.info("Retrieved {} business values for release {}", businessValues.size(), releaseId);

        return businessValues.stream().map(this::mapToResponseWithIssues).collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public BusinessValueResponse getBusinessValueById(UUID id) throws BusinessValueNotFoundException {
        log.info("Fetching business value with id: {}", id);

        Optional<BusinessValue> businessValue = businessValueRepository.findById(id);
        if (businessValue.isEmpty()) {
            throw new BusinessValueNotFoundException("Business value with id " + id + " not found");
        }

        log.info(
                "Found business value '{}' with {} connected issues",
                businessValue.get().getTitle(),
                businessValue.get().getIssues().size());

        return mapToResponseWithIssues(businessValue.get());
    }

    @Transactional
    public BusinessValueResponse createBusinessValue(BusinessValueRequest request)
            throws BusinessValueAlreadyExistsException {
        log.info("Creating business value with name: {}", request.title());

        Optional<BusinessValue> existing = businessValueRepository.findByTitle(request.title());
        if (existing.isPresent()) {
            throw new BusinessValueAlreadyExistsException(
                    "Business value with name '" + request.title() + "' already exists");
        }

        BusinessValue businessValue = mapper.toEntity(request, BusinessValue.class);
        BusinessValue savedBusinessValue = businessValueRepository.save(businessValue);
        log.info(
                "Successfully created business value with id: {} and name: {}",
                savedBusinessValue.getId(),
                savedBusinessValue.getTitle());

        return mapToResponse(savedBusinessValue);
    }

    @Transactional
    public BusinessValueResponse updateBusinessValue(UUID id, BusinessValueRequest request)
            throws BusinessValueNotFoundException, BusinessValueAlreadyExistsException {
        log.info("Updating business value with id: {}", id);

        BusinessValue businessValue = businessValueRepository.findById(id).orElseThrow(() -> {
            log.error("Business value with id {} not found", id);
            return new BusinessValueNotFoundException("Business value with id " + id + " not found");
        });

        if (!businessValue.getTitle().equals(request.title())) {
            Optional<BusinessValue> existing = businessValueRepository.findByTitle(request.title());
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                log.warn("Business value with name '{}' already exists", request.title());
                throw new BusinessValueAlreadyExistsException(
                        "Business value with name '" + request.title() + "' already exists");
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
            Set<String> currentIssueIds =
                    businessValue.getIssues().stream().map(Issue::getId).collect(Collectors.toSet());

            log.info("Disconnecting {} existing issues before connecting new ones", currentIssueIds.size());
            executeDisconnectIssues(businessValueId, currentIssueIds);
        }

        log.info(
                "Connecting {} new issues to business value '{}'",
                request.issueIds().size(),
                businessValue.getTitle());
        return executeConnectIssues(businessValueId, request.issueIds());
    }

    /**
     * Extracts unique business value names from a set of issues.
     * @param issues the set of issues
     * @return set of unique business value names
     */
    private Set<String> extractBusinessValueNames(Set<Issue> issues) {
        Set<String> names = issues.stream()
                .map(Issue::getBusinessValue)
                .filter(Objects::nonNull)
                .map(BusinessValue::getTitle)
                .collect(Collectors.toSet());
        log.info("Extracted {} unique business value names from {} issues", names.size(), issues.size());
        return names;
    }

    /**
     * Fetches business value entities by their names.
     * @param names the set of business value names
     * @return set of business value entities
     */
    private Set<BusinessValue> fetchBusinessValuesByNames(Set<String> names) {
        return names.stream()
                .map(businessValueRepository::findByTitle)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private BusinessValueResponse executeConnectIssues(UUID businessValueId, Set<String> issueIds)
            throws BusinessValueNotFoundException, IssueNotFoundException {
        log.info("Internal: Connecting {} issues to business value id: {}", issueIds.size(), businessValueId);

        BusinessValue businessValue = findBusinessValueById(businessValueId);
        Set<Issue> issues = fetchAndValidateIssues(issueIds);
        Set<Issue> allIssues = collectAllIssuesWithSubIssues(issues);

        setBusinessValueOnIssues(allIssues, businessValue);
        issueRepository.saveAll(allIssues);

        if (businessValue.getIssues() == null) {
            businessValue.setIssues(new HashSet<>());
        }
        businessValue.getIssues().addAll(allIssues);

        return mapToResponseWithIssues(businessValue);
    }

    private void executeDisconnectIssues(UUID businessValueId, Set<String> issueIds)
            throws BusinessValueNotFoundException {
        log.info("Internal: Disconnecting {} issues from business value id: {}", issueIds.size(), businessValueId);

        BusinessValue businessValue = findBusinessValueById(businessValueId);
        Set<Issue> issues = fetchIssuesByIds(issueIds);
        Set<Issue> allIssues = collectAllIssuesWithSubIssues(issues);

        clearBusinessValueFromIssues(allIssues, businessValue);
        issueRepository.saveAll(allIssues);

        if (businessValue.getIssues() != null) {
            businessValue.getIssues().removeAll(allIssues);
        }
    }

    private BusinessValue findBusinessValueById(UUID id) throws BusinessValueNotFoundException {
        return businessValueRepository
                .findById(id)
                .orElseThrow(() -> new BusinessValueNotFoundException("Business value with id " + id + " not found"));
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

    private Set<Issue> fetchIssuesByIds(Set<String> issueIds) {
        return issueIds.stream()
                .map(issueRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private Set<Issue> collectAllIssuesWithSubIssues(Set<Issue> issues) {
        return issues.stream()
                .flatMap(issue -> collectIssueWithSubIssuesStream(issue).stream())
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

    private Set<Issue> collectIssueWithSubIssuesStream(Issue issue) {
        Set<Issue> result = new HashSet<>();
        result.add(issue);

        if (issue.getSubIssues() != null && !issue.getSubIssues().isEmpty()) {
            Set<Issue> subIssues = issue.getSubIssues().stream()
                    .flatMap(subIssue -> collectIssueWithSubIssuesStream(subIssue).stream())
                    .collect(Collectors.toSet());
            result.addAll(subIssues);
        }

        return result;
    }

    private BusinessValueResponse mapToResponse(BusinessValue businessValue) {
        return new BusinessValueResponse(
                businessValue.getId(), businessValue.getTitle(), businessValue.getDescription(), Set.of());
    }

    private BusinessValueResponse mapToResponseWithIssues(BusinessValue businessValue) {
        Set<IssueResponse> issueResponses =
                (businessValue.getIssues() != null && !businessValue.getIssues().isEmpty())
                        ? businessValue.getIssues().stream()
                                .map(issue -> mapper.toDTO(issue, IssueResponse.class))
                                .collect(Collectors.toSet())
                        : Set.of();

        return new BusinessValueResponse(
                businessValue.getId(), businessValue.getTitle(), businessValue.getDescription(), issueResponses);
    }
}
