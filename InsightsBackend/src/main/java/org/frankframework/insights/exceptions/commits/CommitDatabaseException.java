package org.frankframework.insights.exceptions.commits;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public class CommitDatabaseException extends ApiException {
    public CommitDatabaseException(String message, Throwable cause) {
        super(message, ErrorCode.COMMIT_DATABASE_ERROR, cause);
    }
}
