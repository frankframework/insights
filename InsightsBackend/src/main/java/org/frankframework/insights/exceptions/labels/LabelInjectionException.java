package org.frankframework.insights.exceptions.labels;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public class LabelInjectionException extends ApiException {
    public LabelInjectionException(String message, Throwable cause) {
        super(message, ErrorCode.LABEL_INJECTION_ERROR, cause);
    }
}
