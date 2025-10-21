package org.frankframework.insights.milestone;

import java.util.Collections;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.mapper.MappingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/milestones")
public class MilestoneController {
    private final MilestoneService milestoneService;

    public MilestoneController(MilestoneService milestoneService) {
        this.milestoneService = milestoneService;
    }

    /**
     * Fetches all milestones from the database.
     * @return ResponseEntity containing a set of MilestoneResponse objects
     * @throws MappingException if an error occurs during the mapping process
     */
    @GetMapping()
    public ResponseEntity<Set<MilestoneResponse>> getAllMilestones() throws MappingException {
        Set<MilestoneResponse> openMilestones = milestoneService.getAllMilestones();
        if (openMilestones == null) openMilestones = Collections.emptySet();
        return ResponseEntity.status(HttpStatus.OK).body(openMilestones);
    }
}
