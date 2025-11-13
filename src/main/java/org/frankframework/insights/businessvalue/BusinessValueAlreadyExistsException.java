package org.frankframework.insights.businessvalue;

import org.frankframework.insights.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class BusinessValueAlreadyExistsException extends ApiException {
    public BusinessValueAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
