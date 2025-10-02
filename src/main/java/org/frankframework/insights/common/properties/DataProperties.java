package org.frankframework.insights.common.properties;

import lombok.Getter;

import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "data")
@Getter
@Setter
public class DataProperties {
	private boolean fetchEnabled;
}
