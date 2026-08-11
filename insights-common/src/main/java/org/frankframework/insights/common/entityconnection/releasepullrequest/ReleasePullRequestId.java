package org.frankframework.insights.common.entityconnection.releasepullrequest;

import java.io.Serializable;

public record ReleasePullRequestId(String release, String pullRequest) implements Serializable {}
