package org.frankframework.insights.businessvalue;

import org.frankframework.insights.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when trying to create a business value with a name that already exists
 */
public class BusinessValueAlreadyExistsException extends ApiException {
    public BusinessValueAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
