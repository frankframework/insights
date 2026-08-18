package org.frankframework.insights;

import static org.springframework.web.servlet.function.RequestPredicates.path;
import static org.springframework.web.servlet.function.RouterFunctions.route;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT2H", proxyTargetClass = true)
@ConfigurationPropertiesScan
@EnableWebSecurity
@NullMarked
public class InsightsWebappApplication {
    private static final String FRONTEND_LOCATION = "frontend/";

    public static void main(String[] args) {
        SpringApplication app = configureApplication();
        app.run(args);
    }

    public static SpringApplication configureApplication() {
        return new SpringApplication(InsightsWebappApplication.class);
    }

    @Bean
    RouterFunction<ServerResponse> spaRouter() {
        return spaRouter(FRONTEND_LOCATION);
    }

    public static RouterFunction<ServerResponse> spaRouter(String frontendLocation) {
        String location = frontendLocation.endsWith("/") ? frontendLocation : frontendLocation + "/";
        RequestPredicate clientSideRoute = path("/api/**").or(path("/error")).negate();

        return route().resources("/**", new ClassPathResource(location))
                .resource(clientSideRoute, new ClassPathResource(location + "index.html"))
                .build();
    }
}
