package org.frankframework.insights.configuration;

import lombok.Getter;

import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "graphql.query")
@Getter
@Setter
public class GraphQLQueryProperties {
	private String issues;
}
