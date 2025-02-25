package org.frankframework.insights.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.frankframework.insights.models.Label;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LabelMapper {
    private final ObjectMapper objectMapper;

    @Autowired
    public LabelMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Set<Label> jsonToLabels(JsonNode jsonLabels) {
        return StreamSupport.stream(jsonLabels.spliterator(), false)
                .map(this::jsonToLabel)
                .collect(Collectors.toSet());
    }

    private Label jsonToLabel(JsonNode jsonLabel) {
        try {
            return objectMapper.treeToValue(jsonLabel, Label.class);
        } catch (Exception e) {
            throw new RuntimeException("Error mapping JSON to Label", e);
        }
    }
}
