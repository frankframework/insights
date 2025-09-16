package org.frankframework.insights.common.entityconnection.branchpullrequest;

import java.io.Serializable;

public record BranchPullRequestId(String branch, String pullRequest) implements Serializable {}
