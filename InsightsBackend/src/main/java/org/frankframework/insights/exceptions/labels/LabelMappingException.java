package org.frankframework.insights.exceptions.labels;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public class LabelMappingException extends ApiException {
    public LabelMappingException() {
        super("Failed to map the label DTO's into labels", ErrorCode.LABEL_MAPPING_ERROR);
    }
}
