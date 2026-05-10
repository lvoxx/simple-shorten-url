package io.lvoxx.ssurl.analytics_worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "io.lvoxx.ssurl")
public class AnalyticsWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnalyticsWorkerApplication.class, args);
	}

}
