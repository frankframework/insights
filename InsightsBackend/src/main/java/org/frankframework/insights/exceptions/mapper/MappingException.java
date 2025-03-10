package org.frankframework.insights.exceptions.mapper;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public class MappingException extends ApiException {
    public MappingException(String message, Throwable cause) {
        super(message, ErrorCode.MAPPING_ERROR, cause);
    }
}
