package org.frankframework.insights.exceptions.clients;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public abstract class ApiClientException extends ApiException {
    public ApiClientException(String note, ErrorCode errorCode) {
        super(note, errorCode);
    }
}
