package org.frankframework.insights;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.path;
import static org.springframework.web.servlet.function.RequestPredicates.pathExtension;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT2H")
@ConfigurationPropertiesScan
public class InsightsApplication {
    public static void main(String[] args) {
        SpringApplication app = configureApplication();
        app.run(args);
    }

    public static SpringApplication configureApplication() {
        return new SpringApplication(InsightsApplication.class);
    }


	/**
	 * This is a custom router function to accommodate to our single page application that we serve from this spring boot backend as well.
	 * This RouterFunction will make sure that we serve `frontend/index.html` whenever the path does not start with `/api/`, is not `/error` and does
	 * not have a path extension (to exclude static resources).
	 *
	 * @see <a href="https://github.com/spring-projects/spring-framework/issues/27257">Spring framework issue 27257</a> for more details.
	 */
	@Bean
	RouterFunction<ServerResponse> spaRouter() {
		ClassPathResource index = new ClassPathResource("frontend/index.html");
		RequestPredicate spaPredicate = path("/api/**").or(path("/error")).or(pathExtension(extension -> !extension.isBlank())).negate();
		return route().resource(spaPredicate, index).build();
	}
}
