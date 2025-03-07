package org.frankframework.insights.dto;

import java.util.Set;
import org.frankframework.insights.models.Branch;
import org.frankframework.insights.models.Commit;

public record MatchingBranch(Branch branch, Set<Commit> commits) {}
