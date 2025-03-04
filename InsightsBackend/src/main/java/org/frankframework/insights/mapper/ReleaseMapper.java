package org.frankframework.insights.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.stream.Collectors;
import org.frankframework.insights.dto.ReleaseDTO;
import org.frankframework.insights.models.Release;
import org.springframework.stereotype.Component;

@Component
public class ReleaseMapper {
    private final ObjectMapper objectMapper;

    public ReleaseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Set<Release> toEntity(Set<ReleaseDTO> dtoSet) {
        return dtoSet.stream().map(this::toEntity).collect(Collectors.toSet());
    }

    public Release toEntity(ReleaseDTO dto) {
        return objectMapper.convertValue(dto, Release.class);
    }
}
