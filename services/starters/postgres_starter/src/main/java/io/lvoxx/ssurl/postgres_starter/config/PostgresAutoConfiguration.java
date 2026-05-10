package io.lvoxx.ssurl.postgres_starter.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * Provides default R2DBC pool configuration via application.yaml.
 * Spring Boot's R2dbcAutoConfiguration and R2dbcTransactionManagerAutoConfiguration
 * handle ConnectionFactory, ReactiveTransactionManager, and TransactionalOperator.
 */
@AutoConfiguration
public class PostgresAutoConfiguration {
}
