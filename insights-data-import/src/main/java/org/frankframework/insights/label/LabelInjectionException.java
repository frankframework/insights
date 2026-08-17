package org.frankframework.insights.label;

import org.frankframework.insights.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class LabelInjectionException extends ApiException {
    public LabelInjectionException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
