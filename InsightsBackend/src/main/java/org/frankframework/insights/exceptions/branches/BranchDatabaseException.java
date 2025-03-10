package org.frankframework.insights.exceptions.branches;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public class BranchDatabaseException extends ApiException {
    public BranchDatabaseException(String message, Throwable cause) {
        super(message, ErrorCode.BRANCH_DATABASE_ERROR, cause);
    }
}
