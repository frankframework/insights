package org.frankframework.insights.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.exceptions.mapper.MappingException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Mapper {
    private final ObjectMapper objectMapper;

    public Mapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <Tentity, Tdto> Tentity toEntity(Tdto dto, Class<Tentity> entityClass) {
        return objectMapper.convertValue(dto, entityClass);
    }

    public <Tentity, Tdto> Tdto toDTO(Tentity entity, Class<Tdto> dtoClass) {
        return objectMapper.convertValue(entity, dtoClass);
    }

    public <Tentity, Tdto> Set<Tentity> toEntity(Set<Tdto> dtoSet, Class<Tentity> entityClass) throws MappingException {
        try {
            log.info("Converting {} DTOs to Entities [{}]", dtoSet.size(), entityClass.getSimpleName());
            Set<Tentity> entities =
                    dtoSet.stream().map(dto -> toEntity(dto, entityClass)).collect(Collectors.toSet());
            log.info("Successfully mapped {} DTOs to Entities [{}]", dtoSet.size(), entityClass.getSimpleName());
            return entities;
        } catch (Exception e) {
            throw new MappingException("Failed to convert DTOs to Entities: " + e.getMessage(), e);
        }
    }

    public <Tentity, Tdto> Set<Tdto> toDTO(Set<Tentity> entitySet, Class<Tdto> dtoClass) throws MappingException {
        try {
            log.info("Converting {} Entities to DTOs [{}]", entitySet.size(), dtoClass.getSimpleName());
            Set<Tdto> DTOs =
                    entitySet.stream().map(entity -> toDTO(entity, dtoClass)).collect(Collectors.toSet());
            log.info("Successfully mapped {} Entities to DTOs [{}]", entitySet.size(), dtoClass.getSimpleName());
            return DTOs;
        } catch (Exception e) {
            throw new MappingException("Failed to convert Entities to DTOs: " + e.getMessage(), e);
        }
    }
}
