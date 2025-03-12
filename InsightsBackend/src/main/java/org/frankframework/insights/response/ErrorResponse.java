package org.frankframework.insights.response;

import java.util.List;

public record ErrorResponse(Integer httpStatus, List<String> messages, String errorCode) {}
