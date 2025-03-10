package org.frankframework.insights.exceptions.releases;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public class ReleaseDatabaseException extends ApiException {
    public ReleaseDatabaseException(String message, Throwable cause) {
        super(message, ErrorCode.RELEASE_DATABASE_ERROR, cause);
    }
}
