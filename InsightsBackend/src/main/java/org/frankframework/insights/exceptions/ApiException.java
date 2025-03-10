package org.frankframework.insights.exceptions;

import lombok.Getter;
import org.frankframework.insights.enums.ErrorCode;

@Getter
public abstract class ApiException extends Exception {
    private final ErrorCode errorCode;

    public ApiException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApiException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
