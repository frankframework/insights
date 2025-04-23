package org.frankframework.insights.common.configuration;

import java.util.List;
import org.frankframework.insights.common.configuration.properties.CorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    private final String[] allowedOrigins;

    public CorsConfig(CorsProperties corsProperties) {
        allowedOrigins = corsProperties.getOrigins();
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowCredentials(true);
        corsConfig.setAllowedOrigins(List.of(allowedOrigins));
        corsConfig.setAllowedMethods(List.of("GET"));
        corsConfig.setAllowedHeaders(List.of("Origin", "Content-Type", "Accept"));
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsFilter(source);
    }
}
