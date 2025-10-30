package pl.matgwiazda.integration;

// Shared Testcontainers Postgres base for integration tests

import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Testcontainers
public abstract class IntegrationTestBase {

    // Use singleton container to avoid races between test classes
    static {
        // ensure container is started before Spring TestContext registers properties
        TestPostgresContainer.getInstance().start();
    }

    protected static final TestPostgresContainer POSTGRES = TestPostgresContainer.getInstance();

    // Removed explicit start/stop to let Testcontainers manage lifecycle for the static container
    // This avoids starting/stopping the container per test class which can lead to Spring context
    // re-use problems when Gradle forks tests into multiple JVMs.

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }
}
