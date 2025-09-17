package org.frankframework.insights.common.entityconnection.pullrequestissue;

import java.io.Serializable;

public record PullRequestIssueId(String pullRequest, String issue) implements Serializable {}
