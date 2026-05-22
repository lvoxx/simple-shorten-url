package io.lvoxx.ssurl.analytics_worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "io.lvoxx.ssurl")
@EnableConfigurationProperties
public class AnalyticsWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnalyticsWorkerApplication.class, args);
	}

}
