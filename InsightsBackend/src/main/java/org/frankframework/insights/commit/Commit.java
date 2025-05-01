package org.frankframework.insights.commit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Commit {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String sha;

	@Lob
	@Column(nullable = false)
    private String message;

    private OffsetDateTime committedDate;
}
