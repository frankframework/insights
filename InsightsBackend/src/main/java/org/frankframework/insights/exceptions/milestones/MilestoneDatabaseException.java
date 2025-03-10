package org.frankframework.insights.exceptions.milestones;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public class MilestoneDatabaseException extends ApiException {
    public MilestoneDatabaseException(String message, Throwable cause) {
        super(message, ErrorCode.MILESTONE_DATABASE_ERROR, cause);
    }
}
