package org.frankframework.insights.shedlock;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "shedlock")
public class ShedLock {
	@Id
	@Column(length = 64, nullable = false)
	private String name;

	private OffsetDateTime lockUntil;

	@Column(name = "locked_at", nullable = false)
	private OffsetDateTime lockedAt;

	@Column(nullable = false)
	private String lockedBy;
}
