package org.frankframework.webapp.common.entityconnection.pullrequestlabel;

import java.io.Serializable;

public record PullRequestLabelId(String pullRequest, String label) implements Serializable {}
