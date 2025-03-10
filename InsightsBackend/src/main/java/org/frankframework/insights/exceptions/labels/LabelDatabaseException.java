package org.frankframework.insights.exceptions.labels;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public class LabelDatabaseException extends ApiException {
    public LabelDatabaseException(String message, Throwable cause) {
        super(message, ErrorCode.LABEL_DATABASE_ERROR, cause);
    }
}
