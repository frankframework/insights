package org.frankframework.insights.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.stream.Collectors;
import org.frankframework.insights.dto.LabelDTO;
import org.frankframework.insights.models.Label;
import org.springframework.stereotype.Component;

@Component
public class LabelMapper {
    private final ObjectMapper objectMapper;

    public LabelMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Set<Label> toEntity(Set<LabelDTO> dtoSet) {
        return dtoSet.stream().map(this::toEntity).collect(Collectors.toSet());
    }

    public Label toEntity(LabelDTO dto) {
        return objectMapper.convertValue(dto, Label.class);
    }
}
