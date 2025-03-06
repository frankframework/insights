package org.frankframework.insights.exceptions.milestones;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public class MilestoneDatabaseException extends ApiException {
    public MilestoneDatabaseException() {
        super("Failed to insert the milestones into the database", ErrorCode.MILESTONE_DATABASE_ERROR);
    }
}
