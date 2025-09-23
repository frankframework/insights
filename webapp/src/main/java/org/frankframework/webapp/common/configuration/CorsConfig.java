package org.frankframework.webapp.common.configuration;

import java.util.List;
import org.frankframework.webapp.common.configuration.properties.CorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    private final List<String> allowedOrigins;
    private static final long MAX_CORS_CONFIGURATION_CONNECTION = 3600L;

    public CorsConfig(CorsProperties corsProperties) {
        this.allowedOrigins = corsProperties.getOrigins();
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowCredentials(true);
        corsConfig.setAllowedOrigins(allowedOrigins);
        corsConfig.setAllowedMethods(List.of("GET"));
        corsConfig.setAllowedHeaders(List.of("Origin", "Content-Type", "Accept"));
        corsConfig.setMaxAge(MAX_CORS_CONFIGURATION_CONNECTION);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsFilter(source);
    }
}
