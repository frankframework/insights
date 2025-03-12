package org.frankframework.insights.exceptions.labels;

import org.frankframework.insights.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class LabelInjectionException extends ApiException {
    public LabelInjectionException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
