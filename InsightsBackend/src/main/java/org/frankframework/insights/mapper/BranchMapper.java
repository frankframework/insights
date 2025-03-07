package org.frankframework.insights.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.stream.Collectors;
import org.frankframework.insights.dto.BranchDTO;
import org.frankframework.insights.models.Branch;
import org.springframework.stereotype.Component;

@Component
public class BranchMapper {
    private final ObjectMapper objectMapper;

    public BranchMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Set<Branch> toEntity(Set<BranchDTO> dtoSet) {
        return dtoSet.stream().map(this::toEntity).collect(Collectors.toSet());
    }

    public Branch toEntity(BranchDTO dto) {
        return objectMapper.convertValue(dto, Branch.class);
    }
}
