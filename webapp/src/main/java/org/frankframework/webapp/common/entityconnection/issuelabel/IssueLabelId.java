package org.frankframework.webapp.common.entityconnection.issuelabel;

import java.io.Serializable;

public record IssueLabelId(String issue, String label) implements Serializable {}
