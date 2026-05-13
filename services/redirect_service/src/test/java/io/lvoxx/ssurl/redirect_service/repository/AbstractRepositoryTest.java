package io.lvoxx.ssurl.redirect_service.repository;

import io.lvoxx.ssurl.test_starter.AbstractPostgresContainer;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcTransactionManagerAutoConfiguration;
import org.springframework.boot.data.r2dbc.autoconfigure.DataR2dbcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import io.r2dbc.spi.ConnectionFactory;

@Tag("integration")
@SpringBootTest(
    classes = AbstractRepositoryTest.TestR2dbcConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Import(AbstractRepositoryTest.SchemaInitConfig.class)
public abstract class AbstractRepositoryTest extends AbstractPostgresContainer {

    @Configuration
    @ImportAutoConfiguration({
        R2dbcAutoConfiguration.class,
        R2dbcTransactionManagerAutoConfiguration.class,
        DataR2dbcAutoConfiguration.class
    })
    @EnableR2dbcRepositories(basePackages = "io.lvoxx.ssurl.redirect_service.repository")
    static class TestR2dbcConfig {
    }

    @Configuration
    static class SchemaInitConfig {
        @Bean
        public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
            var initializer = new ConnectionFactoryInitializer();
            initializer.setConnectionFactory(connectionFactory);
            initializer.setDatabasePopulator(
                    new ResourceDatabasePopulator(new ClassPathResource("sql/init-schema.sql")));
            return initializer;
        }
    }
}
