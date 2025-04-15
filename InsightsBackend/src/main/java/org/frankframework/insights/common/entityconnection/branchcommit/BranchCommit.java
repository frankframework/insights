package org.frankframework.insights.common.entityconnection.branchcommit;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.commit.Commit;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
public class BranchCommit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
	@JoinColumn(nullable = false)
    private Branch branch;

	@ManyToOne(cascade = { CascadeType.MERGE })
	@JoinColumn(nullable = false)
	private Commit commit;

    public BranchCommit(Branch branch, Commit commit) {
        this.branch = branch;
        this.commit = commit;
    }
}
