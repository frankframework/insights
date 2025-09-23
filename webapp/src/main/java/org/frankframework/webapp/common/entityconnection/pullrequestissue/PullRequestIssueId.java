package org.frankframework.webapp.common.entityconnection.pullrequestissue;

import java.io.Serializable;

public record PullRequestIssueId(String pullRequest, String issue) implements Serializable {}
