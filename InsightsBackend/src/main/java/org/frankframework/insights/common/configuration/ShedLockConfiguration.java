package org.frankframework.insights.common.configuration;

import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class ShedLockConfiguration {
	@Bean
	public JdbcTemplateLockProvider lockProvider(DataSource dataSource) {
		return new JdbcTemplateLockProvider(dataSource);
	}
}
