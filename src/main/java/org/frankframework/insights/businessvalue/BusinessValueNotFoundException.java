package org.frankframework.insights.businessvalue;

import org.frankframework.insights.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class BusinessValueNotFoundException extends ApiException {
    public BusinessValueNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
