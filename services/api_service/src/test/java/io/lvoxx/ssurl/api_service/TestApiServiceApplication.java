package io.lvoxx.ssurl.api_service;

import org.springframework.boot.SpringApplication;

public class TestApiServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(ApiServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
