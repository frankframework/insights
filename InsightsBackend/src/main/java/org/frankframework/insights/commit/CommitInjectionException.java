package org.frankframework.insights.commit;

import org.frankframework.insights.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class CommitInjectionException extends ApiException {
    public CommitInjectionException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
