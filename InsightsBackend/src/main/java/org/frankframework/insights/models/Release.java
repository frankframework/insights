package org.frankframework.insights.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

import org.frankframework.insights.dto.TagCommitDTO;

@Entity
@Table(name = "release")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Release {
    @Id
    private String id;

    @Column(name = "tag_name", nullable = false, unique = true)
    private String tagName;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "published_at", columnDefinition = "TIMESTAMP")
    private OffsetDateTime publishedAt;

	@Column(nullable = false)
	private String oid;

    @ManyToOne
    private Branch branch;

    @ManyToMany
    @JoinTable(
            name = "release_commit",
            joinColumns = @JoinColumn(name = "release_id"),
            inverseJoinColumns = @JoinColumn(name = "commit_id"))
    private Set<Commit> releaseCommits;

	@JsonSetter("tagCommit")
	public void setOidFromTagCommit(TagCommitDTO tagCommitDTO) {
		this.oid = tagCommitDTO.getOid();
	}
}
