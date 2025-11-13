package org.frankframework.insights.businessvalue;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.issue.IssueNotFoundException;
import org.frankframework.insights.issue.IssueRepository;
import org.frankframework.insights.issue.IssueResponse;
import org.frankframework.insights.issue.IssueService;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class BusinessValueService {

    private final BusinessValueRepository businessValueRepository;
    private final IssueRepository issueRepository;
    private final IssueService issueService;
    private final Mapper mapper;

    public BusinessValueService(
            BusinessValueRepository businessValueRepository,
            IssueRepository issueRepository,
            IssueService issueService,
            Mapper mapper) {
        this.businessValueRepository = businessValueRepository;
        this.issueRepository = issueRepository;
        this.issueService = issueService;
        this.mapper = mapper;
    }

    /**
     * Retrieves all unique business values associated with issues in a specific release.
     * This is a public endpoint that doesn't require authentication.
     * Uses IssueService to get issues by release, then collects their business values.
     * @param releaseId the ID of the release
     * @return set of business value responses associated with the release's issues
     * @throws ReleaseNotFoundException if the release is not found
     */
    @Transactional(readOnly = true)
    public Set<BusinessValueResponse> getBusinessValuesByReleaseId(String releaseId)
            throws ReleaseNotFoundException {
        log.info("Fetching business values for release with id: {}", releaseId);

        Set<IssueResponse> issueResponses = issueService.getIssuesByReleaseId(releaseId);
        Set<String> businessValueNames = extractBusinessValueNames(issueResponses);
        Set<BusinessValue> businessValues = fetchBusinessValuesByNames(businessValueNames);

        log.info("Retrieved {} business values for release {}", businessValues.size(), releaseId);

        return businessValues.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toSet());
    }

    /**
     * Creates a new business value.
     * @param request the business value request containing name and description
     * @return the created business value response
     * @throws BusinessValueAlreadyExistsException if a business value with the same name already exists
     */
    @Transactional
    public BusinessValueResponse createBusinessValue(BusinessValueRequest request)
            throws BusinessValueAlreadyExistsException {
        log.info("Creating business value with name: {}", request.name());

        Optional<BusinessValue> existing = businessValueRepository.findByName(request.name());
		if (existing.isPresent()) {
            throw new BusinessValueAlreadyExistsException(
                    "Business value with name '" + request.name() + "' already exists");
        }


        BusinessValue businessValue = mapper.toEntity(request, BusinessValue.class);
        BusinessValue savedBusinessValue = businessValueRepository.save(businessValue);
        log.info("Successfully created business value with id: {} and name: {}",
                savedBusinessValue.getId(), savedBusinessValue.getName());

        return mapToResponse(savedBusinessValue);
    }

    /**
     * Retrieves a business value by ID with all connected issues.
     * @param id the UUID of the business value
     * @return the business value response with connected issues
     * @throws BusinessValueNotFoundException if the business value is not found
     */
    @Transactional(readOnly = true)
    public BusinessValueResponse getBusinessValueById(UUID id) throws BusinessValueNotFoundException {
        log.info("Fetching business value with id: {}", id);

        Optional<BusinessValue> businessValue = businessValueRepository.findById(id);
		if (businessValue.isEmpty()) {
			throw new BusinessValueNotFoundException("Business value with id " + id + " not found");
		}

        log.info("Found business value '{}' with {} connected issues",
                businessValue.get().getName(), businessValue.get().getIssues().size());

        return mapToResponseWithIssues(businessValue.get());
    }

    /**
     * Retrieves all business values.
     * @return set of all business value responses
     */
    @Transactional(readOnly = true)
    public Set<BusinessValueResponse> getAllBusinessValues() {
        log.info("Fetching all business values");

        List<BusinessValue> businessValues = businessValueRepository.findAll();
        log.info("Found {} business values", businessValues.size());

        return businessValues.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toSet());
    }

    /**
     * Connects issues to a business value. If an issue has sub-issues, all sub-issues
     * are also connected to the business value recursively.
     * @param businessValueId the UUID of the business value
     * @param request the request containing issue IDs to connect
     * @return the updated business value response with connected issues
     * @throws BusinessValueNotFoundException if the business value is not found
     * @throws IssueNotFoundException if any of the issues are not found
     */
    @Transactional
    public BusinessValueResponse connectIssuesToBusinessValue(UUID businessValueId, ConnectIssuesRequest request)
            throws BusinessValueNotFoundException, IssueNotFoundException {
        log.info("Connecting {} issues to business value with id: {}", request.issueIds().size(), businessValueId);

        BusinessValue businessValue = findBusinessValueById(businessValueId);
        Set<Issue> issues = fetchAndValidateIssues(request.issueIds());
        Set<Issue> allIssues = collectAllIssuesWithSubIssues(issues);

        setBusinessValueOnIssues(allIssues, businessValue);
        issueRepository.saveAll(allIssues);

        log.info("Successfully connected {} issues to business value '{}'", allIssues.size(), businessValue.getName());

        return mapToResponseWithIssues(businessValueRepository.findById(businessValueId).orElseThrow());
    }

    /**
     * Updates business value details (name and/or description).
     * @param id the UUID of the business value
     * @param request the request containing updated details
     * @return the updated business value response
     * @throws BusinessValueNotFoundException if the business value is not found
     * @throws BusinessValueAlreadyExistsException if updating to a name that already exists
     */
    @Transactional
    public BusinessValueResponse updateBusinessValue(UUID id, BusinessValueRequest request)
            throws BusinessValueNotFoundException, BusinessValueAlreadyExistsException {
        log.info("Updating business value with id: {}", id);

        BusinessValue businessValue = businessValueRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Business value with id {} not found", id);
                    return new BusinessValueNotFoundException("Business value with id " + id + " not found");
                });

        if (!businessValue.getName().equals(request.name())) {
            Optional<BusinessValue> existing = businessValueRepository.findByName(request.name());
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                log.warn("Business value with name '{}' already exists", request.name());
                throw new BusinessValueAlreadyExistsException(
                        "Business value with name '" + request.name() + "' already exists");
            }
        }

        BusinessValue updatedBusinessValue = businessValueRepository.save(mapper.toEntity(request, BusinessValue.class));
        log.info("Successfully updated business value with id: {}", id);

        return mapToResponse(updatedBusinessValue);
    }

    /**
     * Disconnects specific issues from a business value.
     * If an issue has sub-issues, all sub-issues are also disconnected.
     * @param businessValueId the UUID of the business value
     * @param issueIds the set of issue IDs to disconnect
     * @return the updated business value response
     * @throws BusinessValueNotFoundException if the business value is not found
     */
    public BusinessValueResponse disconnectIssuesFromBusinessValue(UUID businessValueId, Set<String> issueIds)
            throws BusinessValueNotFoundException {
        log.info("Disconnecting {} issues from business value with id: {}", issueIds.size(), businessValueId);

        BusinessValue businessValue = findBusinessValueById(businessValueId);
        Set<Issue> issues = fetchIssuesByIds(issueIds);
        Set<Issue> allIssues = collectAllIssuesWithSubIssues(issues);

        clearBusinessValueFromIssues(allIssues, businessValue);
        issueRepository.saveAll(allIssues);

        log.info("Successfully disconnected {} issues from business value '{}'", allIssues.size(), businessValue.getName());

        return mapToResponseWithIssues(businessValueRepository.findById(businessValueId).orElseThrow());
    }

    /**
     * Replaces all issue connections for a business value.
     * Disconnects all currently connected issues and connects the new ones.
     * If an issue has sub-issues, all sub-issues are also connected automatically.
     * @param businessValueId the UUID of the business value
     * @param request the request containing new issue IDs to connect
     * @return the updated business value response with new connections
     * @throws BusinessValueNotFoundException if the business value is not found
     * @throws IssueNotFoundException if any of the new issues are not found
     */
    @Transactional
    public BusinessValueResponse replaceIssueConnections(UUID businessValueId, ConnectIssuesRequest request)
            throws BusinessValueNotFoundException, IssueNotFoundException {
        log.info("Replacing issue connections for business value with id: {}", businessValueId);

        BusinessValue businessValue = findBusinessValueById(businessValueId);
        disconnectAllCurrentIssues(businessValueId, businessValue);

        log.info("Connecting {} new issues to business value '{}'", request.issueIds().size(), businessValue.getName());
        return connectIssuesToBusinessValue(businessValueId, request);
    }

    /**
     * Finds a business value by ID or throws exception.
     */
    private BusinessValue findBusinessValueById(UUID id) throws BusinessValueNotFoundException {
        return businessValueRepository.findById(id)
                .orElseThrow(() -> new BusinessValueNotFoundException("Business value with id " + id + " not found"));
    }

    /**
     * Disconnects all currently connected issues from a business value.
     */
    private void disconnectAllCurrentIssues(UUID businessValueId, BusinessValue businessValue)
            throws BusinessValueNotFoundException {
        if (businessValue.getIssues() != null && !businessValue.getIssues().isEmpty()) {
            Set<String> currentIssueIds = businessValue.getIssues().stream()
                    .map(Issue::getId)
                    .collect(Collectors.toSet());

            log.info("Disconnecting {} existing issues before connecting new ones", currentIssueIds.size());
            disconnectIssuesFromBusinessValue(businessValueId, currentIssueIds);
        }
    }

    private Set<String> extractBusinessValueNames(Set<IssueResponse> issueResponses) {
        Set<String> names = issueResponses.stream()
                .map(IssueResponse::getBusinessValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        log.info("Extracted {} unique business value names from {} issues", names.size(), issueResponses.size());
        return names;
    }

    private Set<BusinessValue> fetchBusinessValuesByNames(Set<String> names) {
        return names.stream()
                .map(businessValueRepository::findByName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private Set<Issue> fetchAndValidateIssues(Set<String> issueIds) throws IssueNotFoundException {
        Map<String, Optional<Issue>> issueMap = issueIds.stream()
                .collect(Collectors.toMap(id -> id, issueRepository::findById));

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
        Set<Issue> allIssues = issues.stream()
                .flatMap(issue -> collectIssueWithSubIssuesStream(issue).stream())
                .collect(Collectors.toSet());
        log.info("Collected {} total issues (including sub-issues) from {} root issues",
                allIssues.size(), issues.size());
        return allIssues;
    }

    private void setBusinessValueOnIssues(Set<Issue> issues, BusinessValue businessValue) {
        log.info("Setting business value '{}' on {} issues", businessValue.getName(), issues.size());
        issues.forEach(issue -> issue.setBusinessValue(businessValue));
    }

    private void clearBusinessValueFromIssues(Set<Issue> issues, BusinessValue businessValue) {
        log.info("Clearing business value '{}' from {} issues", businessValue.getName(), issues.size());
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

    /**
     * Maps a BusinessValue entity to a BusinessValueResponse without issues.
     * @param businessValue the business value entity
     * @return the business value response
     */
    private BusinessValueResponse mapToResponse(BusinessValue businessValue) {
        return new BusinessValueResponse(
                businessValue.getId(),
                businessValue.getName(),
                businessValue.getDescription(),
                Set.of()
        );
    }

    /**
     * Maps a BusinessValue entity to a BusinessValueResponse with all connected issues.
     * @param businessValue the business value entity
     * @return the business value response with issues
     */
    private BusinessValueResponse mapToResponseWithIssues(BusinessValue businessValue) {
        Set<IssueResponse> issueResponses = (businessValue.getIssues() != null && !businessValue.getIssues().isEmpty())
                ? businessValue.getIssues().stream()
                        .map(issue -> mapper.toDTO(issue, IssueResponse.class))
                        .collect(Collectors.toSet())
                : Set.of();

        return new BusinessValueResponse(
                businessValue.getId(),
                businessValue.getName(),
                businessValue.getDescription(),
                issueResponses
        );
    }
}
