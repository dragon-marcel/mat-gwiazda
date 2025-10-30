package pl.matgwiazda.integration;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

/**
 * Singleton Testcontainers Postgres instance for the whole test JVM.
 * Ensures there is a single started container and avoids races/restarts between test classes.
 */
public class TestPostgresContainer extends PostgreSQLContainer<TestPostgresContainer> {

    private static final String IMAGE = "postgres:15-alpine";
    private static final TestPostgresContainer INSTANCE = new TestPostgresContainer();

    private TestPostgresContainer() {
        super(IMAGE);
        withDatabaseName("testdb");
        withUsername("postgres");
        withPassword("postgres");
        // increase startup wait a bit
        waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));
        // prevent Testcontainers from removing container between tests if reuse is enabled elsewhere
        withReuse(false);
    }

    public static TestPostgresContainer getInstance() {
        return INSTANCE;
    }

    @Override
    public void start() {
        // idempotent start: only start if not already running
        if (!INSTANCE.isRunning()) {
            super.start();
        }
    }

    @Override
    public void stop() {
        // noop - do not stop container between tests; let JVM exit and Testcontainers handle cleanup
    }
}

