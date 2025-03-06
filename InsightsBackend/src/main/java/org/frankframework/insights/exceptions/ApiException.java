package org.frankframework.insights.exceptions;

import lombok.Getter;
import org.frankframework.insights.enums.ErrorCode;

@Getter
public abstract class ApiException extends Exception {
    private final String note;
    private final ErrorCode errorCode;

    public ApiException(String note, ErrorCode errorCode) {
        this.note = note;
        this.errorCode = errorCode;
    }
}
