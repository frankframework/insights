package org.frankframework.insights.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Configuration
public class GraphQLQueries {
	private static final String QUERIES_FILE = "/queries.json";

	private final ObjectMapper objectMapper;

	public GraphQLQueries(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Bean
	public List<JsonNode> loadGraphQLQueries() throws IOException {
		InputStream inputStream = getClass().getResourceAsStream(QUERIES_FILE);
		return objectMapper.readValue(inputStream, objectMapper.getTypeFactory().constructCollectionType(List.class, JsonNode.class));
	}
}
