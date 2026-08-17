package org.frankframework.insights.milestone;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.springframework.stereotype.Service;

/**
 * Service class for reading milestones from the database.
 * Filling the milestone table is the responsibility of the data import module.
 */
@Service
@Slf4j
public class MilestoneQueryService {

    private final Mapper mapper;

    private final MilestoneRepository milestoneRepository;

    public MilestoneQueryService(Mapper mapper, MilestoneRepository milestoneRepository) {
        this.mapper = mapper;
        this.milestoneRepository = milestoneRepository;
    }

    /**
     * Fetches all open milestones from the database and returns them as a set of MilestoneResponse objects.
     * @return a set of all open milestones
     * @throws MappingException if an error occurs during the mapping process
     */
    public Set<MilestoneResponse> getAllMilestones() throws MappingException {
        List<Milestone> milestones = milestoneRepository.findAll();
        log.info("Successfully fetched {} milestones from database", milestones.size());
        return mapper.toDTO(new HashSet<>(milestones), MilestoneResponse.class);
    }

    /**
     * Checks if a milestone with the given ID exists in the database.
     * @param milestoneId the ID of the milestone to check
     * @return the Milestone object if it exists
     * @throws MilestoneNotFoundException if the milestone does not exist
     */
    public Milestone checkIfMilestoneExists(String milestoneId) throws MilestoneNotFoundException {
        Optional<Milestone> milestone = milestoneRepository.findById(milestoneId);
        if (milestone.isEmpty()) {
            throw new MilestoneNotFoundException("Milestone with ID [" + milestoneId + "] not found.", null);
        }

        return milestone.get();
    }
}
