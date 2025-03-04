package org.frankframework.insights.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.frankframework.insights.dto.LabelDTO;
import org.frankframework.insights.dto.MilestoneDTO;
import org.frankframework.insights.models.Label;
import org.frankframework.insights.models.Milestone;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class LabelMapper {
	private final ObjectMapper objectMapper;

	public LabelMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Set<Label> toEntity(Set<LabelDTO> dtoSet) {
		return dtoSet.stream()
				.map(this::toEntity)
				.collect(Collectors.toSet());
	}

	public Label toEntity(LabelDTO dto) {
		return objectMapper.convertValue(dto, Label.class);
	}
}
