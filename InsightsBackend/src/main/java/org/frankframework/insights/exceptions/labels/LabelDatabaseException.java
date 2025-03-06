package org.frankframework.insights.exceptions.labels;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public class LabelDatabaseException extends ApiException {
    public LabelDatabaseException() {
        super("Failed to insert the labels into the database", ErrorCode.LABEL_DATABASE_ERROR);
    }
}
