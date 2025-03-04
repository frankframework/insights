package org.frankframework.insights.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.frankframework.insights.dto.CommitDTO;
import org.frankframework.insights.models.Commit;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CommitMapper {
	private final ObjectMapper objectMapper;

	public CommitMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Set<Commit> toEntity(Set<CommitDTO> dtoSet) {
		return dtoSet.stream()
				.map(this::toEntity)
				.collect(Collectors.toSet());
	}

	//todo set

	public Commit toEntity(CommitDTO dto) {
		return objectMapper.convertValue(dto, Commit.class);
	}
}
