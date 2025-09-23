package org.frankframework.webapp.common.entityconnection.releasepullrequest;

import java.io.Serializable;

public record ReleasePullRequestId(String release, String pullRequest) implements Serializable {}
