package org.frankframework.insights.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class GitHubProjectItemDTO {
    public GitHubEdgesDTO<FieldValue> fieldValues;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FieldValue {
        public String optionId;
        public Double number;
        public Field field;

        public static class Field {
            public String name;
        }
    }
}
