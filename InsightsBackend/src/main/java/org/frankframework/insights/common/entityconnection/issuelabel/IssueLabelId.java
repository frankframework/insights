package org.frankframework.insights.common.entityconnection.issuelabel;

import java.io.Serializable;

public record IssueLabelId(String issue, String label) implements Serializable {}
