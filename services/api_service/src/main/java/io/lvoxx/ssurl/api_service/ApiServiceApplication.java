package io.lvoxx.ssurl.api_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "io.lvoxx.ssurl")
@EnableConfigurationProperties
public class ApiServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiServiceApplication.class, args);
	}

}
