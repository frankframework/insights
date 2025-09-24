package org.frankframework.insights.common.entityconnection.releasedependency;

import java.io.Serializable;
import java.util.UUID;

public record ReleaseDependencyId(String release, UUID dependency) implements Serializable {}
