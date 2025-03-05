package org.frankframework.insights.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.stream.Collectors;
import org.frankframework.insights.dto.MilestoneDTO;
import org.frankframework.insights.models.Milestone;
import org.springframework.stereotype.Component;

@Component
public class MilestoneMapper {
    private final ObjectMapper objectMapper;

    public MilestoneMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Set<Milestone> toEntity(Set<MilestoneDTO> dtoSet) {
        return dtoSet.stream().map(this::toEntity).collect(Collectors.toSet());
    }

    public Milestone toEntity(MilestoneDTO dto) {
        return objectMapper.convertValue(dto, Milestone.class);
    }
}
