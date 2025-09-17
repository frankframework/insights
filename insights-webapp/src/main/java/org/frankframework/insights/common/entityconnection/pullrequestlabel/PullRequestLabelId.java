package org.frankframework.insights.common.entityconnection.pullrequestlabel;

import java.io.Serializable;

public record PullRequestLabelId(String pullRequest, String label) implements Serializable {}
