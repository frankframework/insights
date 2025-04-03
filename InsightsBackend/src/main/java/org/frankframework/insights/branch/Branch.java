package org.frankframework.insights.branch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommit;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Branch {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    private Set<BranchCommit> branchCommits = new HashSet<>();

    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    private Set<BranchPullRequest> branchPullRequests = new HashSet<>();
}
