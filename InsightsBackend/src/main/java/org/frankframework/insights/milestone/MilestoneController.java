package org.frankframework.insights.milestone;

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
@RequestMapping("/api/milestones")
public class MilestoneController {
    private final MilestoneService milestoneService;

    public MilestoneController(MilestoneService milestoneService) {
        this.milestoneService = milestoneService;
    }

    @GetMapping("/open")
    public ResponseEntity<Set<MilestoneResponse>> getAllOpenMilestones() throws MappingException {
        Set<MilestoneResponse> openMilestones = milestoneService.getAllOpenMilestones();
        return ResponseEntity.status(HttpStatus.OK).body(openMilestones);
    }
}
