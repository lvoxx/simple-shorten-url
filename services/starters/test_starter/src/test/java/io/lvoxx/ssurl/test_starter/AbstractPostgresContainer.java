package io.lvoxx.ssurl.test_starter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.lvoxx.ssurl.test_starter.config.NoCacheLoadConfig;

@SuppressWarnings("resource")
@Testcontainers
@Import(NoCacheLoadConfig.class)
public class AbstractPostgresContainer {
    @Container
    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:17.4-alpine")
                .withDatabaseName("test")
                .withUsername("root")
                .withPassword("Te3tP4ssW@r$")
                // .withInitScript("customer_test.sql")
                .withReuse(true);
    }

    @DynamicPropertySource
    static void configureR2dbc(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url",
                () -> String.format("r2dbc:postgresql://%s:%d/%s",
                        POSTGRES.getHost(),
                        POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                        POSTGRES.getDatabaseName()));
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    }

    @BeforeAll
    static void setUp() {
        POSTGRES.start();
    }

    @AfterAll
    static void tearDown() {
        POSTGRES.stop();
    }
}
