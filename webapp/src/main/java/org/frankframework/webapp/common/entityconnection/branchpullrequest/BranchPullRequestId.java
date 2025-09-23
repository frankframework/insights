package org.frankframework.webapp.common.entityconnection.branchpullrequest;

import java.io.Serializable;

public record BranchPullRequestId(String branch, String pullRequest) implements Serializable {}
