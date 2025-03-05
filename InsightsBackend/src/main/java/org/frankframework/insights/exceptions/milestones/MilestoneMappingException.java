package org.frankframework.insights.exceptions.milestones;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public class MilestoneMappingException extends ApiException {
    public MilestoneMappingException() {
        super("Failed to map the milestone DTO's into milestones", ErrorCode.MILESTONE_MAPPING_ERROR);
    }
}
